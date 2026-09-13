package com.wms.engine.config;

import com.wms.engine.service.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Ouvinte de eventos de segurança do Spring Security.
 * Responsável por capturar tentativas de login e notificar o serviço de controle,
 * servindo como base para mecanismos de prevenção contra ataques de força bruta (Brute-force).
 */
@Component
public class AuthenticationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final LoginAttemptService loginAttemptService;

    public AuthenticationEventListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Captura eventos de falha por credenciais inválidas (senha errada).
     * Incrementa o contador de falhas do usuário para possível bloqueio temporário.
     *
     * @param event Evento disparado automaticamente pelo Spring Security na falha
     */
    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof String username) {
            log.warn("Auditoria de Segurança: Falha de autenticação detectada para o usuário [{}]", username);
            loginAttemptService.loginFailed(username);
        }
    }

    /**
     * Captura eventos de autenticação bem-sucedida.
     * Zera o contador de falhas, garantindo que o usuário legítimo não seja penalizado
     * por erros antigos.
     *
     * @param event Evento disparado automaticamente pelo Spring Security no sucesso
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            log.info("Auditoria de Segurança: Autenticação realizada com sucesso para o usuário [{}]", username);
            loginAttemptService.loginSucceeded(username);
        }
    }
}