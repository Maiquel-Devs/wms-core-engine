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

/**
 * Suíte de testes unitários para o cliente de Inteligência Artificial ({@link AiAllocationClient}).
 * Garante que as integrações externas e os mecanismos de contingência (fallback)
 * funcionem corretamente em cenários adversos, como a ausência de chaves de API.
 */
@ExtendWith(MockitoExtension.class)
class AiAllocationClientTest {

    @Mock
    private ParametroSistemaService parametroService;

    private AiAllocationClient client;

    @BeforeEach
    void setUp() {
        client = new AiAllocationClient(parametroService);
    }

    /**
     * Testa o comportamento de segurança do cliente.
     * Se o administrador do sistema não tiver configurado uma API Key válida no painel,
     * a aplicação NÃO deve lançar exceção. Em vez disso, deve interceptar o erro e
     * retornar um payload JSON de contingência acionando o motor local.
     */
    @Test
    @DisplayName("Deve acionar fallback limpo sem lançar exceção quando a API key estiver em branco")
    void deveAcionarFallbackQuandoApiKeyVazia() {
        // 1. Arrange (Preparação do cenário)
        when(parametroService.obterValorPorChave(eq("AI_PROVIDER"), eq("MISTRAL")))
                .thenReturn("MISTRAL");

        when(parametroService.obterValorPorChave(eq("AI_API_KEY"), eq("")))
                .thenReturn("");

        String contextoSimulado = "Contexto de teste do armazém com 1 palete e 5 vagas limitadas.";

        // 2. Act (Execução da ação a ser testada)
        String jsonResposta = client.obterSugestaoAlocacao(contextoSimulado);

        // 3. Assert (Verificação dos resultados esperados com mensagens claras para o Log de CI/CD)
        assertNotNull(
                jsonResposta,
                "A resposta do cliente de IA não pode ser nula, mesmo acionando o fallback."
        );

        assertTrue(
                jsonResposta.contains("MOTOR_HEURISTICO_LOCAL"),
                "O JSON retornado deve indicar explicitamente que a decisão foi tomada pelo motor de contingência local."
        );

        assertTrue(
                jsonResposta.contains("C1"),
                "O JSON de fallback deve conter um código de vaga simulado padrão (ex: C1) para evitar falhas no conversor JSON."
        );
    }
}