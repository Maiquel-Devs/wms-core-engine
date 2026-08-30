package com.wms.engine.repository;

import com.wms.engine.model.Palete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaleteRepository extends JpaRepository<Palete, Long> {

    Optional<Palete> findByCodigoLote(String codigoLote);

    // Busca paletes que já foram recebidos mas ainda não foram endereçados no armazém
    List<Palete> findByEnderecoIsNull();

    // Paletes alocados nas estantes
    List<Palete> findByEnderecoIsNotNull();
}