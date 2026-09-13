package com.wms.engine.dto.ia;

import java.util.List;

/**
 * Data Transfer Object (DTO) que representa o payload de resposta recebido de provedores externos de IA.
 * Mapeia a estrutura padrão de resposta JSON compatível com a API da OpenAI/Mistral.
 *
 * @param choices A lista de opções (choices) de conclusão geradas retornadas pelo modelo de IA.
 */
public record AiResponse(List<Choice> choices) {

    /**
     * Representa uma única opção de conclusão fornecida pelo modelo de IA dentro da resposta.
     *
     * @param message A mensagem gerada contendo o conteúdo da resposta da IA e o seu papel (role).
     */
    public record Choice(AiMessage message) {
    }
}