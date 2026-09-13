package com.wms.engine.repository;

import com.wms.engine.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório responsável pelas operações de persistência e consultas da entidade {@link Usuario}.
 * Essencial para a integração com os mecanismos de autenticação e autorização (Spring Security),
 * gerenciando o acesso dos operadores e administradores do WMS.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário pelo seu nome de acesso (login), garantindo no nível do banco de dados
     * que apenas usuários com o status ativo (ativo = true) sejam retornados.
     * Utilizado principalmente no processo de autenticação (UserDetailsService) para bloquear
     * automaticamente a entrada de contas desativadas ou suspensas.
     *
     * @param username O nome de usuário (login) informado durante a tentativa de acesso.
     * @return Um {@link Optional} contendo o usuário caso ele exista e esteja ativo, ou vazio caso contrário.
     */
    Optional<Usuario> findByUsernameAndAtivoTrue(String username);
}