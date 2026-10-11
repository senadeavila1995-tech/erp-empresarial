package com.erp.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoInventarioRequest(
        @NotNull
        TipoMovimiento tipo,

        @NotNull
        @Min(0)
        Integer cantidad,

        @NotBlank
        @Size(max = 500)
        String motivo
) {
}
