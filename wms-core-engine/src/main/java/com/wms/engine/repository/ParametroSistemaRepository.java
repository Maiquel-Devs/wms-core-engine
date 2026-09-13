package com.wms.engine.repository;

import com.wms.engine.model.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório responsável pelas operações de persistência e consultas da entidade {@link ParametroSistema}.
 * Permite o acesso dinâmico e centralizado às configurações globais do WMS armazenadas no banco de dados.
 */
@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {

    /**
     * Busca a configuração de um parâmetro do sistema pela sua chave única de identificação.
     * Retorna um {@link Optional} para facilitar o tratamento seguro na camada de serviço
     * caso o parâmetro não esteja cadastrado na base de dados (evitando NullPointerException).
     *
     * @param chave A chave de identificação do parâmetro desejado (ex: "IA_PROMPT_ALOCACAO").
     * @return Um {@link Optional} contendo o parâmetro caso seja encontrado, ou vazio caso contrário.
     */
    Optional<ParametroSistema> findByChave(String chave);
}