package com.wms.engine;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe base abstrata para toda a suíte de testes de integração do WMS.
 * Utiliza o Testcontainers para provisionar um banco de dados PostgreSQL efêmero via Docker.
 * Centralizar essa configuração garante consistência ambiental e evita a poluição do banco de desenvolvimento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    /**
     * Inicializa o contêiner do PostgreSQL na versão 16 (imagem Alpine otimizada para leveza).
     * O modificador 'static' é crucial aqui: ele aplica o padrão Singleton, garantindo que
     * o contêiner suba apenas uma vez e seja reaproveitado por todas as classes de teste,
     * reduzindo drasticamente o tempo total de execução da pipeline de CI/CD.
     */
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wms_test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    /**
     * Intercepta e sobrescreve dinamicamente as propriedades do 'application-test.yml' em tempo de execução.
     * Aponta as conexões do sistema e do motor de migração para as portas dinâmicas mapeadas pelo Docker.
     *
     * @param registry O registro de propriedades dinâmicas do Spring Context.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        // 1. Configurações do Pool de Conexões Principal (JPA/Hibernate)
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // 2. Configurações do Motor de Migração Estrutural (Flyway)
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
}