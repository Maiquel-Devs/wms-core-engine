package com.wms.engine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de segurança executado em todas as requisições HTTP (Hardening).
 * Responsável por injetar cabeçalhos de proteção recomendados pela OWASP
 * e ocultar detalhes da infraestrutura (Information Leakage).
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // Constantes para os cabeçalhos de segurança
    private static final String HEADER_SERVER = "Server";
    private static final String HEADER_POWERED_BY = "X-Powered-By";
    private static final String HEADER_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String HEADER_FRAME_OPTIONS = "X-Frame-Options";

    private static final String VALUE_NOSNIFF = "nosniff";
    private static final String VALUE_DENY = "DENY";
    private static final String VALUE_EMPTY = "";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Prevenção contra Vazamento de Informação (Information Leakage)
        // Oculta a tecnologia e o servidor subjacente para dificultar ataques direcionados
        response.setHeader(HEADER_SERVER, VALUE_EMPTY);
        response.setHeader(HEADER_POWERED_BY, VALUE_EMPTY);

        // 2. Prevenção contra MIME Sniffing
        // Impede que o navegador tente adivinhar o tipo do conteúdo, forçando o respeito ao Content-Type
        response.setHeader(HEADER_CONTENT_TYPE_OPTIONS, VALUE_NOSNIFF);

        // 3. Prevenção contra Clickjacking (Sequestro de Clique)
        // Impede que a aplicação seja renderizada dentro de um <iframe> malicioso em outro domínio
        response.setHeader(HEADER_FRAME_OPTIONS, VALUE_DENY);

        // Continua o fluxo normal da requisição para os próximos filtros/controllers
        filterChain.doFilter(request, response);
    }
}