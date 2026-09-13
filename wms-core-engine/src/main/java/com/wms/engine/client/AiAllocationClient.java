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

/**
 * Cliente responsável pela comunicação com provedores de Inteligência Artificial (OpenAI / Mistral).
 * Isola a complexidade da requisição HTTP e garante a continuidade operacional do WMS
 * através de um mecanismo de fallback heurístico caso a IA falhe.
 */
@Component
public class AiAllocationClient {

    private static final Logger log = LoggerFactory.getLogger(AiAllocationClient.class);
    private static final Marker AUDIT_MARKER = MarkerFactory.getMarker("LOG_AUDIT_LOGISTICA");

    // Parâmetros de Configuração
    private static final String PARAM_PROVIDER = "AI_PROVIDER";
    private static final String PARAM_API_KEY = "AI_API_KEY";
    private static final String PROVIDER_OPENAI = "OPENAI";
    private static final String PROVIDER_MISTRAL = "MISTRAL";

    // Endpoints e Modelos
    private static final String URL_OPENAI = "https://api.openai.com/v1/chat/completions";
    private static final String URL_MISTRAL = "https://api.mistral.ai/v1/chat/completions";
    private static final String MODEL_OPENAI = "gpt-4o-mini";
    private static final String MODEL_MISTRAL = "mistral-small-latest";
    private static final double TEMPERATURE_BAIXA = 0.1; // Foco em respostas determinísticas e não criativas

    private static final String PROMPT_SISTEMA = "Você é um especialista em logística e alocação de WMS. " +
            "Responda ESTRITAMENTE em formato JSON puro, sem blocos markdown: " +
            "{\"vagaSugerida\": \"C1\", \"justificativa\": \"...\"}";

    private final RestClient restClient;
    private final ParametroSistemaService parametroService;

    public AiAllocationClient(ParametroSistemaService parametroService) {
        this.restClient = RestClient.create();
        this.parametroService = parametroService;
    }

    /**
     * Solicita ao provedor de IA ativo a melhor sugestão de vaga com base no contexto físico do armazém.
     *
     * @param contextoArmazem Dados em texto contendo o palete atual e as vagas disponíveis
     * @return String JSON contendo a vagaSugerida e justificativa, pronta para desserialização
     */
    public String obterSugestaoAlocacao(String contextoArmazem) {
        String apiKey = parametroService.obterValorPorChave(PARAM_API_KEY, "");

        // Guard clause: Se não houver chave configurada, não tenta bater na rede, sai rápido
        if (apiKey.isBlank()) {
            log.warn(AUDIT_MARKER, "Chave de IA não configurada. Ativando fallback heurístico de segurança.");
            return executarFallbackHeuristico();
        }

        String provider = parametroService.obterValorPorChave(PARAM_PROVIDER, PROVIDER_MISTRAL);
        String url = resolverUrl(provider);
        String modelo = resolverModelo(provider);

        AiRequest request = montarCorpoRequisicao(modelo, contextoArmazem);

        try {
            return executarChamadaHttp(url, apiKey, request, provider);
        } catch (Exception e) {
            log.error(AUDIT_MARKER, "Falha na chamada ao provedor {}. Causa detalhada: {}", provider, e.getMessage());
            return executarFallbackHeuristico();
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES E ISOLAMENTO DE LÓGICA
    // ==========================================

    private String resolverUrl(String provider) {
        return PROVIDER_OPENAI.equalsIgnoreCase(provider) ? URL_OPENAI : URL_MISTRAL;
    }

    private String resolverModelo(String provider) {
        return PROVIDER_OPENAI.equalsIgnoreCase(provider) ? MODEL_OPENAI : MODEL_MISTRAL;
    }

    private AiRequest montarCorpoRequisicao(String modelo, String contextoArmazem) {
        AiMessage systemMessage = new AiMessage("system", PROMPT_SISTEMA);
        AiMessage userMessage = new AiMessage("user", contextoArmazem);

        return new AiRequest(modelo, List.of(systemMessage, userMessage), TEMPERATURE_BAIXA);
    }

    private String executarChamadaHttp(String url, String apiKey, AiRequest request, String provider) {
        AiResponse response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiResponse.class);

        if (response == null || response.choices().isEmpty()) {
            log.warn(AUDIT_MARKER, "Resposta vazia retornada pelo provedor {}. Executando fallback.", provider);
            return executarFallbackHeuristico();
        }

        String conteudo = response.choices().get(0).message().content();
        return higienizarRespostaJson(conteudo);
    }

    /**
     * Remove formatação Markdown residual.
     * Algumas IAs (mesmo instruídas no prompt) podem retornar o JSON encapsulado em crases ```json
     */
    private String higienizarRespostaJson(String conteudoCru) {
        if (conteudoCru == null) {
            return executarFallbackHeuristico();
        }
        return conteudoCru.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    /**
     * Fallback Heurístico Local (Engine Determinística).
     * Garante que o armazém continue operando caso a API externa sofra timeout, queda ou chave inválida.
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