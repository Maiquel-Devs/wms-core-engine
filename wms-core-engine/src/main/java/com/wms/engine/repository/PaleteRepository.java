package com.wms.engine.repository;

import com.wms.engine.model.Palete;
import com.wms.engine.model.StatusPalete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaleteRepository extends JpaRepository<Palete, Long> {

    List<Palete> findByStatus(StatusPalete status);

    List<Palete> findTop10ByStatusOrderByExpedidoEmDesc(StatusPalete status);

    boolean existsByCodigoLote(String codigoLote);
}