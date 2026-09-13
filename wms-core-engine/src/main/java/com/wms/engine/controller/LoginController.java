package com.wms.engine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador responsável por renderizar a página de autenticação da aplicação.
 * As restrições de segurança e o roteamento para este endpoint são gerenciados pelo SecurityConfig.
 */
@Controller
public class LoginController {

    // Constantes para evitar magic strings no roteamento e resolução da view
    private static final String ROUTE_LOGIN = "/login";
    private static final String VIEW_LOGIN = "login";

    /**
     * Renderiza o formulário de login customizado.
     *
     * @return o nome lógico da view para o template Thymeleaf de login
     */
    @GetMapping(ROUTE_LOGIN)
    public String login() {
        return VIEW_LOGIN;
    }
}