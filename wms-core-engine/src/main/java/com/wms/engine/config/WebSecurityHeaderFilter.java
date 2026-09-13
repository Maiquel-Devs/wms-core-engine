package com.wms.engine.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse) {
                @Override
                public void setHeader(String name, String value) {
                    if ("Server".equalsIgnoreCase(name) || "X-Powered-By".equalsIgnoreCase(name)) {
                        return;
                    }
                    super.setHeader(name, value);
                }

                @Override
                public void addHeader(String name, String value) {
                    if ("Server".equalsIgnoreCase(name) || "X-Powered-By".equalsIgnoreCase(name)) {
                        return;
                    }
                    super.addHeader(name, value);
                }
            };

            chain.doFilter(request, wrapper);
        } else {
            chain.doFilter(request, response);
        }
    }
}