package com.wms.engine.client;

import com.wms.engine.dto.ia.AiMessage;
import com.wms.engine.dto.ia.AiRequest;
import com.wms.engine.dto.ia.AiResponse;
import com.wms.engine.service.ParametroSistemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AiAllocationClient {

    private static final Logger log = LoggerFactory.getLogger(AiAllocationClient.class);
    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("LOG_AUDIT_LOGISTICA");

    private final RestClient restClient;
    private final ParametroSistemaService parametroService;

    public AiAllocationClient(ParametroSistemaService parametroService) {
        this.restClient = RestClient.create();
        this.parametroService = parametroService;
    }

    public String obterSugestaoAlocacao(String contextoArmazem) {
        String provider = parametroService.obterValorPorChave("AI_PROVIDER", "MISTRAL");
        String apiKey = parametroService.obterValorPorChave("AI_API_KEY", "");

        if (apiKey.isBlank()) {
            log.warn(AUDIT_MARKER, "Chave de IA não configurada. Ativando fallback heurístico de segurança.");
            return executarFallbackHeuristico();
        }

        String url;
        String modelo;

        if ("OPENAI".equalsIgnoreCase(provider)) {
            url = "https://api.openai.com/v1/chat/completions";
            modelo = "gpt-4o-mini";
        } else {
            url = "https://api.mistral.ai/v1/chat/completions";
            modelo = "mistral-small-latest";
        }

        AiMessage systemMessage = new AiMessage("system",
                "Você é um especialista em logística e alocação de WMS. " +
                        "Responda ESTRITAMENTE em formato JSON puro, sem blocos markdown: " +
                        "{\"vagaSugerida\": \"C1\", \"justificativa\": \"...\"}");
        AiMessage userMessage = new AiMessage("user", contextoArmazem);

        AiRequest requestBody = new AiRequest(modelo, List.of(systemMessage, userMessage), 0.1);

        try {
            AiResponse response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(AiResponse.class);

            if (response != null && !response.choices().isEmpty()) {
                String conteudo = response.choices().get(0).message().content();
                return conteudo.replace("```json", "").replace("```", "").trim();
            }

            log.warn(AUDIT_MARKER, "Resposta vazia retornada pela API externa. Executando fallback.");
            return executarFallbackHeuristico();

        } catch (Exception e) {
            log.error(AUDIT_MARKER, "Falha na chamada ao provedor {}. Causa detalhada: {}", provider, e.getMessage());
            return executarFallbackHeuristico();
        }
    }

    /**
     * Fallback Heurístico Local (Engine Determinística)
     * Resposta limpa e orientada à operação logística, sem expor detalhes internos ou mensagens de erro.
     */
    private String executarFallbackHeuristico() {
        return """
            {
              "vagaSugerida": "C1",
              "origemDecisao": "MOTOR_HEURISTICO_LOCAL",
              "justificativa": "Alocação emergencial de contingência na baia de químicos e solo (C1) acionada preventivamente por indisponibilidade temporária do serviço de IA."
            }
            """;
    }
}