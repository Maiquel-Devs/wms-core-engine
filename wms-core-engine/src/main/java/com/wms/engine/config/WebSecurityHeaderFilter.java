package com.wms.engine.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de mais alta precedência projetado para interceptar a resposta HTTP
 * e suprimir cabeçalhos de identificação injetados automaticamente pelo Servidor
 * de Aplicação (ex: Tomcat/Undertow), prevenindo Information Leakage.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityHeaderFilter implements Filter {

    private static final String HEADER_SERVER = "Server";
    private static final String HEADER_POWERED_BY = "X-Powered-By";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Cláusula de guarda: Se não for uma requisição HTTP padrão, segue o fluxo sem interferência
        if (!(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // Envolve a resposta original com o decorator para blindar a escrita de cabeçalhos
        HttpServletResponseWrapper wrapper = new HeaderStrippingResponseWrapper(httpResponse);
        chain.doFilter(request, wrapper);
    }

    /**
     * Wrapper interno (Decorator) para interceptar os métodos de resposta HTTP.
     * Ignora silenciosamente qualquer tentativa do container de adicionar cabeçalhos sensíveis.
     */
    private static class HeaderStrippingResponseWrapper extends HttpServletResponseWrapper {

        public HeaderStrippingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            if (isHeaderRestrito(name)) {
                return; // Aborta a injeção do cabeçalho
            }
            super.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            if (isHeaderRestrito(name)) {
                return; // Aborta a injeção do cabeçalho
            }
            super.addHeader(name, value);
        }

        private boolean isHeaderRestrito(String name) {
            return HEADER_SERVER.equalsIgnoreCase(name) || HEADER_POWERED_BY.equalsIgnoreCase(name);
        }
    }
}