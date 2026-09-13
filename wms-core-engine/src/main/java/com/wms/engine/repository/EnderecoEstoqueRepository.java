package com.wms.engine.repository;

import com.wms.engine.model.EnderecoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório responsável pelas operações de persistência e consultas da entidade {@link EnderecoEstoque}.
 * Fornece as queries de busca essenciais para o funcionamento do motor de alocação de paletes.
 */
@Repository
public interface EnderecoEstoqueRepository extends JpaRepository<EnderecoEstoque, Long> {

    /**
     * Busca um endereço físico específico pelo seu código identificador único.
     *
     * @param codigoEndereco O código do endereço (ex: R01-B01-N01-P01).
     * @return Um {@link Optional} contendo o endereço físico caso ele exista.
     */
    Optional<EnderecoEstoque> findByCodigoEndereco(String codigoEndereco);

    /**
     * Retorna uma lista de todas as vagas atualmente desocupadas no armazém.
     * A ordenação prioriza os níveis mais baixos (chão) e posições menores para
     * otimizar o tempo de empilhadeira na operação logística.
     *
     * @return Lista de endereços de estoque desocupados, ordenados de baixo para cima.
     */
    List<EnderecoEstoque> findByOcupadoFalseOrderByNivelAscPosicaoAsc();

    /**
     * Consulta central do motor de alocação heurístico.
     * Busca vagas livres que suportem o peso e o volume físicos exigidos pelo palete,
     * permitindo filtrar opcionalmente por um nível específico da estante.
     *
     * @param peso         O peso total do palete em quilogramas (kg) a ser validado contra a capacidade da vaga.
     * @param volume       O volume total do palete em metros cúbicos (m³) a ser validado contra a capacidade da vaga.
     * @param nivelExigido (Opcional) O nível específico exigido (ex: 1 para produtos muito pesados que devem ficar no chão).
     * @return Lista de vagas elegíveis ordenadas de forma otimizada (nível e posição).
     */
    @Query("""
        SELECT e FROM EnderecoEstoque e 
        WHERE e.ocupado = false 
          AND e.capacidadePesoKg >= :peso 
          AND e.capacidadeVolumeM3 >= :volume
          AND (:nivelExigido IS NULL OR e.nivel = :nivelExigido)
        ORDER BY e.nivel ASC, e.posicao ASC
    """)
    List<EnderecoEstoque> buscarVagasDisponiveis(
            @Param("peso") BigDecimal peso,
            @Param("volume") BigDecimal volume,
            @Param("nivelExigido") Integer nivelExigido
    );
}