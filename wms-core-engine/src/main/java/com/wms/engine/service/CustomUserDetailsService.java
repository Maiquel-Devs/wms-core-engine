package com.wms.engine.service;

import com.wms.engine.model.Usuario;
import com.wms.engine.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Serviço de integração customizado para o Spring Security.
 * Atua como a ponte entre o mecanismo de autenticação do framework e a nossa
 * base de dados de usuários do WMS.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca as credenciais do usuário pelo login (username) para validar a sessão.
     * A regra de bloqueio de contas inativas é garantida no nível da query do banco de dados
     * (findByUsernameAndAtivoTrue).
     *
     * @param username O nome de acesso preenchido no formulário de login.
     * @return Um objeto {@link UserDetails} contendo login, senha hash e o perfil de acesso (Role).
     * @throws UsernameNotFoundException Se o usuário não for encontrado ou estiver com status inativo.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Usuário não encontrado ou inativo: %s", username)
                ));

        return new User(
                usuario.getUsername(),
                usuario.getSenha(),
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getPerfil()))
        );
    }
}