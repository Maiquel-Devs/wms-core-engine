package com.wms.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.engine.client.AiAllocationClient;
import com.wms.engine.dto.ia.AlocacaoDecisaoDTO;
import com.wms.engine.exception.CapacidadeExcedidaException;
import com.wms.engine.exception.EnderecoOcupadoException;
import com.wms.engine.model.EnderecoEstoque;
import com.wms.engine.model.Palete;
import com.wms.engine.model.StatusPalete;
import com.wms.engine.repository.EnderecoEstoqueRepository;
import com.wms.engine.repository.PaleteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlocacaoService {

    private static final Logger log = LoggerFactory.getLogger(AlocacaoService.class);

    private static final BigDecimal LIMITE_PESO_NIVEL_SUPERIOR = new BigDecimal("500.00");
    private static final Integer NIVEL_PISO = 1;

    private final EnderecoEstoqueRepository enderecoRepository;
    private final PaleteRepository paleteRepository;
    private final AiAllocationClient aiClient;
    private final ObjectMapper objectMapper;

    public AlocacaoService(EnderecoEstoqueRepository enderecoRepository,
                           PaleteRepository paleteRepository,
                           AiAllocationClient aiClient) {
        this.enderecoRepository = enderecoRepository;
        this.paleteRepository = paleteRepository;
        this.aiClient = aiClient;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(readOnly = true)
    public AlocacaoDecisaoDTO sugerirVagaInteligente(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        if (palete.getEndereco() != null || palete.getStatus() != StatusPalete.RECEBIDO_DOCA) {
            throw new IllegalStateException("Apenas paletes pendentes na doca podem receber sugestão de vaga.");
        }

        Integer nivelExigido = palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0 ? NIVEL_PISO : null;
        List<EnderecoEstoque> vagasCompativeis = enderecoRepository.buscarVagasDisponiveis(
                palete.getPesoTotalKg(),
                palete.getVolumeTotalM3(),
                nivelExigido
        );

        if (vagasCompativeis.isEmpty()) {
            throw new CapacidadeExcedidaException(
                    "Nenhuma posição física compatível encontrada para o palete (Peso: " +
                            palete.getPesoTotalKg() + "kg, Volume: " + palete.getVolumeTotalM3() + "m³)"
            );
        }

        EnderecoEstoque melhorVagaReal = vagasCompativeis.get(0);

        try {
            String contextoArmazem = montarPromptContexto(palete, vagasCompativeis);
            String respostaJson = aiClient.obterSugestaoAlocacao(contextoArmazem);
            AlocacaoDecisaoDTO decisao = objectMapper.readValue(respostaJson, AlocacaoDecisaoDTO.class);

            boolean vagaValidaNoArmazem = vagasCompativeis.stream()
                    .anyMatch(v -> v.getCodigoEndereco().equalsIgnoreCase(decisao.vagaSugerida()));

            if (!vagaValidaNoArmazem) {
                return new AlocacaoDecisaoDTO(
                        melhorVagaReal.getCodigoEndereco(),
                        "MOTOR_HEURISTICO_LOCAL",
                        String.format("Alocação direta de contingência no solo (Posição: %s, Nível %d) devido à indisponibilidade temporária da IA.",
                                melhorVagaReal.getCodigoEndereco(), melhorVagaReal.getNivel())
                );
            }

            return decisao;

        } catch (Exception e) {
            log.warn("Acionando fallback determinístico local: {}", e.getMessage());
            return new AlocacaoDecisaoDTO(
                    melhorVagaReal.getCodigoEndereco(),
                    "MOTOR_HEURISTICO_LOCAL",
                    String.format("Alocação determinística na vaga %s (Nível %d) baseada em capacidade de carga e cubagem física.",
                            melhorVagaReal.getCodigoEndereco(), melhorVagaReal.getNivel())
            );
        }
    }

    private String montarPromptContexto(Palete palete, List<EnderecoEstoque> vagas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Lote em doca:\n");
        sb.append(String.format("- Lote: %s\n", palete.getCodigoLote()));
        sb.append(String.format("- Produto: %s\n", palete.getProduto() != null ? palete.getProduto().getNome() : "Não especificado"));
        sb.append(String.format("- Peso: %s kg\n", palete.getPesoTotalKg()));
        sb.append(String.format("- Volume: %s m³\n\n", palete.getVolumeTotalM3()));

        sb.append("Posições físicas disponíveis homologadas no armazém:\n");
        for (EnderecoEstoque vaga : vagas) {
            sb.append(String.format("- Vaga %s (Nível %d, Capacidade Peso: %s kg, Cubagem: %s m³)\n",
                    vaga.getCodigoEndereco(),
                    vaga.getNivel(),
                    vaga.getCapacidadePesoKg(),
                    vaga.getCapacidadeVolumeM3()
            ));
        }

        sb.append("\nRegras de segurança estrutural:\n");
        sb.append("1. Cargas acima de 500 kg obrigatoriamente devem permanecer no solo (Nível 1).\n");
        sb.append("2. Escolha OBRIGATORIAMENTE uma das vagas listadas acima.\n");
        sb.append("3. Responda ESTRITAMENTE em formato JSON puro com o schema: ");
        sb.append("{\"vagaSugerida\": \"CODIGO_DA_VAGA\", \"origemDecisao\": \"IA_LOGISTICA\", \"justificativa\": \"motivo técnico detalhado\"}");

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirMelhorEndereco(Palete palete) {
        Integer nivelExigido = null;

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

    @Transactional
    public Palete alocarPalete(Long paleteId, Long enderecoId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        if (palete.getStatus() == StatusPalete.EXPEDIDO) {
            throw new IllegalStateException("Paletes já expedidos não podem ser realocados.");
        }

        EnderecoEstoque endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado com ID: " + enderecoId));

        if (Boolean.TRUE.equals(endereco.getOcupado())) {
            throw new EnderecoOcupadoException("O endereço " + endereco.getCodigoEndereco() + " já está ocupado.");
        }

        if (palete.getPesoTotalKg().compareTo(endereco.getCapacidadePesoKg()) > 0) {
            throw new CapacidadeExcedidaException(
                    "Peso do palete (" + palete.getPesoTotalKg() + "kg) excede a capacidade do endereço (" +
                            endereco.getCapacidadePesoKg() + "kg)."
            );
        }

        if (palete.getVolumeTotalM3().compareTo(endereco.getCapacidadeVolumeM3()) > 0) {
            throw new CapacidadeExcedidaException(
                    "Volume do palete (" + palete.getVolumeTotalM3() + "m³) excede a cubagem do endereço (" +
                            endereco.getCapacidadeVolumeM3() + "m³)."
            );
        }

        if (palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0 && !endereco.getNivel().equals(NIVEL_PISO)) {
            throw new CapacidadeExcedidaException(
                    "Cargas superiores a " + LIMITE_PESO_NIVEL_SUPERIOR + "kg devem ser alocadas no Nível 1 (Piso)."
            );
        }

        // Transição de estado: RECEBIDO_DOCA -> ARMAZENADO
        endereco.setOcupado(true);
        palete.setEndereco(endereco);
        palete.setStatus(StatusPalete.ARMAZENADO);
        palete.setAlocadoEm(LocalDateTime.now());

        enderecoRepository.save(endereco);
        return paleteRepository.save(palete);
    }

    /**
     * Efetiva a baixa por expedição (Checkout de Saída).
     * Libera o endereço físico e arquiva o registro como EXPEDIDO.
     */
    @Transactional
    public void desalocarPalete(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        EnderecoEstoque endereco = palete.getEndereco();
        if (endereco != null) {
            endereco.setOcupado(false);
            enderecoRepository.save(endereco);
        }

        // Transição de estado: ARMAZENADO -> EXPEDIDO
        palete.setEndereco(null);
        palete.setStatus(StatusPalete.EXPEDIDO);
        palete.setExpedidoEm(LocalDateTime.now());
        paleteRepository.save(palete);
    }

    /**
     * Exclui fisicamente um palete que ainda está aguardando vaga na doca.
     */
    @Transactional
    public void excluirPaleteDaDoca(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        if (palete.getStatus() != StatusPalete.RECEBIDO_DOCA || palete.getEndereco() != null) {
            throw new IllegalStateException("Apenas paletes pendentes na doca podem ser removidos.");
        }

        paleteRepository.delete(palete);
    }

    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirVaga(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException("Palete não encontrado com ID: " + paleteId));

        if (palete.getEndereco() != null || palete.getStatus() != StatusPalete.RECEBIDO_DOCA) {
            throw new IllegalStateException("O palete não está aguardando vaga na doca.");
        }

        return sugerirMelhorEndereco(palete);
    }
}