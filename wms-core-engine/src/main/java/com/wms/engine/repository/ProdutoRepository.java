package com.wms.engine.repository;

import com.wms.engine.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório responsável pelas operações de persistência e consultas da entidade {@link Produto}.
 * Gerencia o acesso ao catálogo de SKUs (Stock Keeping Units) disponíveis no sistema WMS.
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Busca um produto específico no catálogo utilizando o seu código SKU único.
     * Retorna um {@link Optional} para garantir um tratamento seguro na camada de serviço
     * caso o produto solicitado não exista na base de dados.
     *
     * @param codigoSku O código de identificação único do produto (SKU).
     * @return Um {@link Optional} contendo o produto caso seja encontrado, ou vazio caso contrário.
     */
    Optional<Produto> findByCodigoSku(String codigoSku);

    /**
     * Verifica de forma otimizada se já existe um produto cadastrado com o código SKU informado.
     * Muito utilizado em validações (guard clauses) na criação de novos produtos para evitar
     * duplicidade de SKUs no catálogo e violação de restrições de unicidade no banco.
     *
     * @param codigoSku O código SKU a ser verificado.
     * @return {@code true} se o produto já existir no banco de dados, {@code false} caso contrário.
     */
    boolean existsByCodigoSku(String codigoSku);
}