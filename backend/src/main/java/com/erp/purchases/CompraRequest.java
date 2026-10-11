package com.erp.purchases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record CompraRequest(
    @NotNull @Positive Long proveedorId,
    @NotBlank @Size(max = 100) String numeroFactura,
    @NotNull @PastOrPresent LocalDate fechaCompra,
    @NotEmpty List<@Valid Linea> lineas
) {
    public record Linea(
        @NotNull @Positive Long productoId,
        @NotNull @Positive Integer cantidad,
        @NotNull @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2) BigDecimal costoUnitario
    ) {}
}
