package com.wms.engine.dto.ia;

import java.util.List;

/**
 * Data Transfer Object (DTO) que representa a estrutura do payload enviado aos provedores de IA externos.
 * Encapsula o modelo alvo, o histórico de mensagens da conversa e os parâmetros de geração.
 *
 * @param model       O identificador do modelo de IA a ser utilizado (ex: gpt-4o-mini, mistral-small-latest).
 * @param messages    A lista de mensagens de contexto e prompt que formam a thread da conversa.
 * @param temperature Controla a aleatoriedade na resposta gerada (valores menores produzem um comportamento mais determinístico).
 */
public record AiRequest(String model, List<AiMessage> messages, double temperature) {
}