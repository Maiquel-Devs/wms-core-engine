package com.wms.engine.repository;

import com.wms.engine.model.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Long> {
    Optional<ParametroSistema> findByChave(String chave);
}