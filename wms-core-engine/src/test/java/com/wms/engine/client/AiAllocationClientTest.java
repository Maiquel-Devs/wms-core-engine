package com.wms.engine.client;

import com.wms.engine.service.ParametroSistemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAllocationClientTest {

    @Mock
    private ParametroSistemaService parametroService;

    private AiAllocationClient client;

    @BeforeEach
    void setUp() {
        client = new AiAllocationClient(parametroService);
    }

    @Test
    @DisplayName("Deve acionar fallback limpo sem lançar exceção quando a API key estiver em branco")
    void deveAcionarFallbackQuandoApiKeyVazia() {
        when(parametroService.obterValorPorChave(eq("AI_PROVIDER"), eq("MISTRAL"))).thenReturn("MISTRAL");
        when(parametroService.obterValorPorChave(eq("AI_API_KEY"), eq(""))).thenReturn("");

        String jsonResposta = client.obterSugestaoAlocacao("Contexto de teste do armazém");

        assertNotNull(jsonResposta);
        assertTrue(jsonResposta.contains("MOTOR_HEURISTICO_LOCAL"));
        assertTrue(jsonResposta.contains("C1"));
    }
}