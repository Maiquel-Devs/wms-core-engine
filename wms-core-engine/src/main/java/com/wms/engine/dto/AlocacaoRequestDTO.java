package com.wms.engine.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) utilizado para as requisições de alocação de estoque.
 * Transporta os dados necessários para vincular um palete pendente na doca a uma vaga física no armazém.
 *
 * @param paleteId   Identificador único do palete que será movimentado.
 * @param enderecoId Identificador único do endereço físico (vaga/estante) de destino.
 */
public record AlocacaoRequestDTO(
        @NotNull(message = "O ID do palete é obrigatório")
        Long paleteId,

        @NotNull(message = "O ID do endereço é obrigatório")
        Long enderecoId
) {}