package com.wms.engine.repository;

import com.wms.engine.model.Palete;
import com.wms.engine.model.StatusPalete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório responsável pelas operações de persistência e consultas da entidade {@link Palete}.
 * Gerencia o acesso aos dados de movimentação e o ciclo de vida das cargas dentro do armazém.
 */
@Repository
public interface PaleteRepository extends JpaRepository<Palete, Long> {

    /**
     * Busca uma lista de paletes com base no seu status operacional atual.
     * Útil para recuperar todas as cargas que estão em uma etapa específica
     * do processo (ex: buscar todos os paletes com status RECEBIDO_DOCA para processamento).
     *
     * @param status O estado operacional alvo da busca.
     * @return Lista de paletes que se encontram no status informado.
     */
    List<Palete> findByStatus(StatusPalete status);

    /**
     * Retorna os 10 paletes mais recentes que atingiram um determinado status,
     * ordenados de forma decrescente pela data de expedição.
     * Frequentemente utilizado para alimentar dashboards de controle e relatórios rápidos.
     *
     * @param status O estado operacional alvo da busca (geralmente EXPEDIDO).
     * @return Lista contendo até 10 paletes ordenados pelas últimas expedições.
     */
    List<Palete> findTop10ByStatusOrderByExpedidoEmDesc(StatusPalete status);

    /**
     * Verifica de forma otimizada se já existe um lote registrado no sistema com o código informado.
     * Fundamental para as "guard clauses" (cláusulas de guarda) na camada de serviço,
     * prevenindo a entrada duplicada de um mesmo palete na doca.
     *
     * @param codigoLote O código único de identificação do palete a ser verificado.
     * @return {@code true} se o lote já estiver cadastrado no banco de dados, {@code false} caso contrário.
     */
    boolean existsByCodigoLote(String codigoLote);
}