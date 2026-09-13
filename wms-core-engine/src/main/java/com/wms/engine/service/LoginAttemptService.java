package com.wms.engine.service;

import com.wms.engine.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável por monitorar tentativas de login e prevenir ataques de força bruta (Brute Force).
 * Realiza o bloqueio preventivo (inativação) de contas após o limite de tentativas falhas ser atingido.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    public static final int MAX_ATTEMPTS = 3;

    /**
     * Cache em memória para rastrear falhas rápidas (thread-safe).
     * Nota: Em um ambiente produtivo escalado horizontalmente, seria ideal migrar isso para o Redis.
     */
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    private final UsuarioRepository usuarioRepository;

    public LoginAttemptService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Limpa o histórico de falhas do usuário no cache após uma autenticação bem-sucedida.
     *
     * @param username O nome de usuário que realizou o login com sucesso.
     */
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
    }

    /**
     * Registra uma tentativa de login falha. Caso o número de falhas atinja o limite máximo configurado,
     * a conta do usuário é inativada no banco de dados de forma transacional.
     *
     * @param username O nome de usuário que tentou acessar o sistema.
     */
    @Transactional
    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0) + 1;
        attemptsCache.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            usuarioRepository.findByUsernameAndAtivoTrue(username).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
                log.warn("[AUDIT LOCKOUT] Conta inativada por excesso de tentativas falhas: {}", username);
            });
        }
    }
}