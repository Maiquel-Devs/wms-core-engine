package com.wms.engine;

import org.springframework.boot.SpringApplication;

/**
 * Ponto de entrada alternativo para inicialização da aplicação em ambiente de desenvolvimento local.
 * Utiliza o recurso "Testcontainers at Development Time" do ecossistema Spring Boot.
 *
 * Ao rodar a aplicação através deste método main (ao invés da {@link WmsCoreEngineApplication} padrão),
 * o Spring injeta a classe {@link TestcontainersConfiguration}, subindo automaticamente os contêineres
 * Docker necessários (ex: PostgreSQL) e configurando as credenciais de banco dinamicamente,
 * sem que o desenvolvedor precise instalar um banco localmente.
 */
public class TestWmsCoreEngineApplication {

	public static void main(String[] args) {
		SpringApplication
				.from(WmsCoreEngineApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}
}