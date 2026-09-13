package com.wms.engine.dto.ia;

/**
 * Data Transfer Object (DTO) que representa uma única mensagem no histórico de conversa com a IA.
 * Utilizado para estruturar o payload enviado aos provedores de IA (ex: OpenAI, Mistral).
 *
 * @param role    O papel do autor da mensagem (ex: "system", "user", "assistant").
 * @param content O conteúdo em texto real ou prompt da mensagem.
 */
public record AiMessage(String role, String content) {
}