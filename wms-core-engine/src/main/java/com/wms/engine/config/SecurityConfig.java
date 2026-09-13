package com.wms.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Configuração central de segurança (Perímetro do WMS).
 * Define o controle de acesso baseado em perfis (RBAC), proteção contra ataques (CSRF)
 * e o ciclo de vida das sessões de autenticação do sistema.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Define o algoritmo de hash para armazenamento seguro das senhas no banco.
     * O BCrypt é o padrão da indústria por incluir "salt" dinâmico e ser resistente a ataques de força bruta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura a esteira de filtros de segurança (Security Filter Chain).
     * Intercepta e processa todas as requisições HTTP antes que cheguem aos Controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Proteção contra CSRF (Cross-Site Request Forgery)
                // O uso de withHttpOnlyFalse permite que o frontend (Javascript/Ajax) consiga ler o token se necessário.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                // 2. Mapeamento de Rotas e Controle de Acesso (RBAC)
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos públicos para renderização correta da tela de login
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/favicon.svg").permitAll()

                        // Rotas públicas
                        .requestMatchers("/login").permitAll()

                        // Rotas restritas aos gestores do sistema
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Rotas do núcleo logístico operacional
                        .requestMatchers("/", "/paletes/**").hasAnyRole("OPERADOR", "ADMIN")

                        // Barreira final: Qualquer outra rota não mapeada exige usuário autenticado
                        .anyRequest().authenticated()
                )

                // 3. Gerenciamento de Sessão Stateful
                // IF_REQUIRED é o padrão ideal para aplicações Web tradicionais (cria sessão apenas após login)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // 4. Configuração do Formulário de Login HTML
                .formLogin(form -> form
                        .loginPage("/login")
                        // O 'true' força o redirecionamento para o painel "/", ignorando URLs anteriores cacheadas
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 5. Encerramento Seguro da Sessão (Logout)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        // Higienização explícita limpando cookies sensíveis do navegador
                        .deleteCookies("APP_SESSION_ID", "XSRF-TOKEN")
                        .permitAll()
                );

        return http.build();
    }
}