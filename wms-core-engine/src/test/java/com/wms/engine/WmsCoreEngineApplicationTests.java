package com.wms.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Teste de sanidade (Smoke Test) estrutural da aplicação WMS Core Engine.
 * Valida se o Spring Application Context consegue inicializar com sucesso, resolvendo
 * todas as injeções de dependência (Beans), propriedades e conexões de banco de dados.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WmsCoreEngineApplicationTests {

	/**
	 * Verifica o carregamento do contexto principal do Spring Boot.
	 * Não possui 'asserts' explícitos (como assertEquals), pois o simples fato do
	 * contexto carregar sem lançar exceções (ApplicationContextException) já configura
	 * o sucesso do teste. Geralmente é o primeiro teste a rodar na esteira de CI/CD.
	 */
	@Test
	@DisplayName("Deve carregar o contexto do Spring Boot com sucesso acionando o Testcontainers")
	void contextLoads() {
		// O escopo de execução chegar aqui significa que o contexto subiu com sucesso.
	}
}