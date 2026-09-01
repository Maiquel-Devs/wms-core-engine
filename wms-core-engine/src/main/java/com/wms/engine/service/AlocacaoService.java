package com.wms.engine.service;

import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.exception.EnderecoOcupadoException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlocacaoService {

    // Limite de segurança: Cargas acima de 500 kg obrigatoriamente vão para o piso (Nível 1)
    private static final BigDecimal LIMITE_PESO_NIVEL_SUPERIOR = new BigDecimal("500.00");
    private static final Integer NIVEL_PISO = 1;

    private final EnderecoEstoqueRepository enderecoRepository;
    private final PaleteRepository paleteRepository;

    public AlocacaoService(EnderecoEstoqueRepository enderecoRepository, PaleteRepository paleteRepository) {
        this.enderecoRepository = enderecoRepository;
        this.paleteRepository = paleteRepository;
    }

    /**
     * Motor de sugestão determinística: Encontra a melhor vaga disponível
     * respeitando restrições de peso, cubagem e segurança estrutural por nível.
     */
    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirMelhorEndereco(Palete palete) {
        Integer nivelExigido = null;

        // Regra física determinística: Carga pesada não pode subir
        if (palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0) {
            nivelExigido = NIVEL_PISO;
        }

        List<EnderecoEstoque> vagas = enderecoRepository.buscarVagasDisponiveis(
                palete.getPesoTotalKg(),
                palete.getVolumeTotalM3(),
                nivelExigido
        );

        return vagas.stream()
                .findFirst()
                .orElseThrow(() -> new CapacidadeExcedidaException(
                        "Nenhuma posição compatível encontrada para o palete (Peso: " +
                                palete.getPesoTotalKg() + "kg, Volume: " + palete.getVolumeTotalM3() + "m³)"
                ));
    }

    /**
     * Efetiva a alocação do palete no endereço com trava transacional (@Transactional).
     * O campo @Version em EnderecoEstoque previne race conditions automaticamente.
     */
    @Transactional
    public Palete alocarPalete(Long paleteId, Long enderecoId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        EnderecoEstoque endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado com ID: " + enderecoId));

        // Validação de ocupação física
        if (Boolean.TRUE.equals(endereco.getOcupado())) {
            throw new EnderecoOcupadoException("O endereço " + endereco.getCodigoEndereco() + " já está ocupado.");
        }

        // Validação física de peso
        if (palete.getPesoTotalKg().compareTo(endereco.getCapacidadePesoKg()) > 0) {
            throw new CapacidadeExcedidaException(
                    "Peso do palete (" + palete.getPesoTotalKg() + "kg) excede a capacidade do endereço (" +
                            endereco.getCapacidadePesoKg() + "kg)."
            );
        }

        // Validação física de cubagem
        if (palete.getVolumeTotalM3().compareTo(endereco.getCapacidadeVolumeM3()) > 0) {
            throw new CapacidadeExcedidaException(
                    "Volume do palete (" + palete.getVolumeTotalM3() + "m³) excede a cubagem do endereço (" +
                            endereco.getCapacidadeVolumeM3() + "m³)."
            );
        }

        // Regra estrutural: Cargas > 500 kg somente no Nível 1
        if (palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0 && !endereco.getNivel().equals(NIVEL_PISO)) {
            throw new CapacidadeExcedidaException(
                    "Cargas superiores a " + LIMITE_PESO_NIVEL_SUPERIOR + "kg devem ser alocadas no Nível 1 (Piso)."
            );
        }

        // Atualização e vinculação
        endereco.setOcupado(true);
        palete.setEndereco(endereco);
        palete.setAlocadoEm(LocalDateTime.now());

        enderecoRepository.save(endereco);
        return paleteRepository.save(palete);
    }

    /**
     * Desocupa o endereço e libera o palete (para expedição ou remanejamento).
     */
    @Transactional
    public void desalocarPalete(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        EnderecoEstoque endereco = palete.getEndereco();
        if (endereco != null) {
            endereco.setOcupado(false);
            enderecoRepository.save(endereco);
            palete.setEndereco(null);
            palete.setAlocadoEm(null); // Limpa o timestamp de alocação ao desocupar
            paleteRepository.save(palete);
        }
    }

    /**
     * Ponto de entrada para sugestão por ID de palete
     */
    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirVaga(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        if (palete.getEndereco() != null) {
            throw new IllegalStateException("O palete já está alocado na vaga: " + palete.getEndereco().getCodigoEndereco());
        }

        return sugerirMelhorEndereco(palete);
    }
}