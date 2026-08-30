package com.wms.engine.repository;

import com.wms.engine.model.EnderecoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoEstoqueRepository extends JpaRepository<EnderecoEstoque, Long> {

    Optional<EnderecoEstoque> findByCodigoEndereco(String codigoEndereco);

    // Busca posições livres ordenadas do nível mais baixo (chão) para o mais alto
    List<EnderecoEstoque> findByOcupadoFalseOrderByNivelAscPosicaoAsc();

    // Query do motor de alocação: busca vagas livres que suportem o peso e volume exigidos
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