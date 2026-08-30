package com.wms.engine.repository;

import com.wms.engine.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByCodigoSku(String codigoSku);

    boolean existsByCodigoSku(String codigoSku);
}