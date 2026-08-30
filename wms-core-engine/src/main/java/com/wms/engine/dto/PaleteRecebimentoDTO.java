package com.wms.engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaleteRecebimentoDTO(
        @NotBlank(message = "O código do lote é obrigatório")
        String codigoLote,

        @NotNull(message = "Selecione um produto mestre")
        Long produtoId,

        @NotNull(message = "Informe a quantidade de itens")
        @Min(value = 1, message = "A quantidade mínima é 1 item")
        Integer quantidadeItens,

        @NotNull(message = "O peso total é obrigatório")
        @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero")
        BigDecimal pesoTotalKg,

        @NotNull(message = "O volume total é obrigatório")
        @DecimalMin(value = "0.0001", message = "O volume deve ser maior que zero")
        BigDecimal volumeTotalM3
) {}