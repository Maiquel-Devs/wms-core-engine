package com.wms.engine.dto.ia;

/**
 * Data Transfer Object (DTO) que encapsula a decisão final de alocação de um palete.
 * Pode ser populado pela resposta do motor de Inteligência Artificial ou pelo
 * fallback heurístico local em caso de contingência.
 *
 * @param vagaSugerida  O código da vaga física recomendada para o armazenamento (ex: "C1").
 * @param origemDecisao O sistema responsável por tomar a decisão (ex: "IA_LOGISTICA" ou "MOTOR_HEURISTICO_LOCAL").
 * @param justificativa A explicação técnica detalhando o motivo da escolha da vaga.
 */
public record AlocacaoDecisaoDTO(
        String vagaSugerida,
        String origemDecisao,
        String justificativa
) {
}