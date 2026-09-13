package com.wms.engine.service;

import com.wms.engine.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 3;
    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final UsuarioRepository usuarioRepository;

    public LoginAttemptService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    @Transactional
    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0) + 1;
        attemptsCache.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            usuarioRepository.findByUsernameAndAtivoTrue(username).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
                System.out.println("[AUDIT LOCKOUT] Conta inativada por excesso de tentativas: " + username);
            });
        }
    }
}