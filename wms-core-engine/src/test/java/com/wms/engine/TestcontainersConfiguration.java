package com.wms.engine;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuração de infraestrutura efêmera (Testcontainers) voltada para testes ou
 * inicialização do ambiente de desenvolvimento local (Testcontainers at Development Time).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * Provisiona um banco de dados PostgreSQL via Docker.
	 * A mágica do {@link ServiceConnection} (Spring Boot 3.1+) injeta automaticamente
	 * a URL JDBC, usuário e senha no Spring Context, dispensando configurações manuais.
	 *
	 * @return O contêiner configurado (travado na versão 16-alpine para garantir estabilidade).
	 */
	@Bean
	@ServiceConnection
	public PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
	}

}