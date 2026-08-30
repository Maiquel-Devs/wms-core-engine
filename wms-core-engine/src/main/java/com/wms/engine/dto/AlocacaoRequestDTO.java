package com.wms.engine.dto;

import jakarta.validation.constraints.NotNull;

public record AlocacaoRequestDTO(
        @NotNull(message = "O ID do palete é obrigatório")
        Long paleteId,

        @NotNull(message = "O ID do endereço é obrigatório")
        Long enderecoId
) {}