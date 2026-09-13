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

/**
 * Serviço principal de domínio (Use Case) responsável por orquestrar o armazenamento e
 * a expedição física das mercadorias.
 * Integra o motor de regras heurísticas locais com o cliente de Inteligência Artificial,
 * garantindo a segurança estrutural do armazém (limites de peso/volume).
 */
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

    /**
     * Solicita uma sugestão otimizada de alocação integrando o cliente de IA.
     * Em caso de falha da IA ou incompatibilidade da vaga sugerida, aciona automaticamente
     * um fallback determinístico (motor heurístico local).
     *
     * @param paleteId Identificador do palete aguardando na doca.
     * @return DTO contendo a decisão de alocação e a justificativa técnica.
     */
    @Transactional(readOnly = true)
    public AlocacaoDecisaoDTO sugerirVagaInteligente(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Palete não encontrado com ID: %d", paleteId)));

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
            throw new CapacidadeExcedidaException(String.format(
                    "Nenhuma posição física compatível encontrada para o palete (Peso: %skg, Volume: %sm³)",
                    palete.getPesoTotalKg(), palete.getVolumeTotalM3()
            ));
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
                        String.format("Alocação direta de contingência no solo (Posição: %s, Nível %d) devido à inconsistência na sugestão da IA.",
                                melhorVagaReal.getCodigoEndereco(), melhorVagaReal.getNivel())
                );
            }

            return decisao;

        } catch (Exception e) {
            log.warn("Falha na integração com IA. Acionando fallback determinístico local: {}", e.getMessage());
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

    /**
     * Executa o motor heurístico local para encontrar a melhor vaga fisicamente compatível.
     *
     * @param palete A entidade palete validada contendo as métricas de peso e cubagem.
     * @return O melhor endereço de estoque disponível.
     * @throws CapacidadeExcedidaException Se não houver vaga que suporte a carga.
     */
    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirMelhorEndereco(Palete palete) {
        Integer nivelExigido = null;

        if (palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0) {
            nivelExigido = NIVEL_PISO;
        }

        return enderecoRepository.buscarVagasDisponiveis(
                palete.getPesoTotalKg(),
                palete.getVolumeTotalM3(),
                nivelExigido
        ).stream().findFirst().orElseThrow(() -> new CapacidadeExcedidaException(String.format(
                "Nenhuma posição compatível encontrada para o palete (Peso: %skg, Volume: %sm³)",
                palete.getPesoTotalKg(), palete.getVolumeTotalM3()
        )));
    }

    /**
     * Executa a transação de alocação de um palete em uma vaga específica.
     * Processa rigorosas validações (guard clauses) de capacidade, volume e regras de piso
     * antes de confirmar o armazenamento.
     *
     * @param paleteId   Identificador do palete na doca.
     * @param enderecoId Identificador do endereço de destino.
     * @return O palete atualizado com o novo status.
     */
    @Transactional
    public Palete alocarPalete(Long paleteId, Long enderecoId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Palete não encontrado com ID: %d", paleteId)));

        if (palete.getStatus() == StatusPalete.EXPEDIDO) {
            throw new IllegalStateException("Paletes já expedidos não podem ser realocados.");
        }

        EnderecoEstoque endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Endereço não encontrado com ID: %d", enderecoId)));

        if (Boolean.TRUE.equals(endereco.getOcupado())) {
            throw new EnderecoOcupadoException(String.format("O endereço %s já está ocupado.", endereco.getCodigoEndereco()));
        }

        if (palete.getPesoTotalKg().compareTo(endereco.getCapacidadePesoKg()) > 0) {
            throw new CapacidadeExcedidaException(String.format(
                    "Peso do palete (%skg) excede a capacidade do endereço (%skg).",
                    palete.getPesoTotalKg(), endereco.getCapacidadePesoKg()
            ));
        }

        if (palete.getVolumeTotalM3().compareTo(endereco.getCapacidadeVolumeM3()) > 0) {
            throw new CapacidadeExcedidaException(String.format(
                    "Volume do palete (%sm³) excede a cubagem do endereço (%sm³).",
                    palete.getVolumeTotalM3(), endereco.getCapacidadeVolumeM3()
            ));
        }

        if (palete.getPesoTotalKg().compareTo(LIMITE_PESO_NIVEL_SUPERIOR) > 0 && !endereco.getNivel().equals(NIVEL_PISO)) {
            throw new CapacidadeExcedidaException(String.format(
                    "Cargas superiores a %skg devem ser alocadas no Nível 1 (Piso).",
                    LIMITE_PESO_NIVEL_SUPERIOR
            ));
        }

        endereco.setOcupado(true);
        palete.setEndereco(endereco);
        palete.setStatus(StatusPalete.ARMAZENADO);
        palete.setAlocadoEm(LocalDateTime.now());

        enderecoRepository.save(endereco);
        return paleteRepository.save(palete);
    }

    /**
     * Efetiva a baixa por expedição (Checkout de Saída).
     * Libera o endereço físico na estante e arquiva o registro da carga como EXPEDIDO.
     *
     * @param paleteId Identificador único do palete a ser despachado.
     */
    @Transactional
    public void desalocarPalete(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Palete não encontrado com ID: %d", paleteId)));

        EnderecoEstoque endereco = palete.getEndereco();
        if (endereco != null) {
            endereco.setOcupado(false);
            enderecoRepository.save(endereco);
        }

        palete.setEndereco(null);
        palete.setStatus(StatusPalete.EXPEDIDO);
        palete.setExpedidoEm(LocalDateTime.now());
        paleteRepository.save(palete);
    }

    /**
     * Exclui fisicamente do banco de dados um palete que ainda está aguardando vaga na doca.
     *
     * @param paleteId Identificador do palete a ser estornado/excluído.
     */
    @Transactional
    public void excluirPaleteDaDoca(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Palete não encontrado com ID: %d", paleteId)));

        if (palete.getStatus() != StatusPalete.RECEBIDO_DOCA || palete.getEndereco() != null) {
            throw new IllegalStateException("Apenas paletes pendentes na doca podem ser removidos.");
        }

        paleteRepository.delete(palete);
    }

    /**
     * Ponto de entrada simplificado para buscar sugestão de vaga baseado apenas no ID do palete.
     * Valida se a carga está elegível para armazenamento antes de acionar o motor de busca.
     *
     * @param paleteId Identificador do palete aguardando na doca.
     * @return O endereço físico (vaga) sugerido pelo motor local.
     */
    @Transactional(readOnly = true)
    public EnderecoEstoque sugerirVaga(Long paleteId) {
        Palete palete = paleteRepository.findById(paleteId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Palete não encontrado com ID: %d", paleteId)));

        if (palete.getEndereco() != null || palete.getStatus() != StatusPalete.RECEBIDO_DOCA) {
            throw new IllegalStateException("O palete não está aguardando vaga na doca.");
        }

        return sugerirMelhorEndereco(palete);
    }
}