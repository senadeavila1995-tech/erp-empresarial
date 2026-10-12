package com.erp.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record VentaRequest(
    @NotBlank @Size(max = 100) String numeroFactura,
    @NotNull @PastOrPresent LocalDate fechaVenta,
    @NotEmpty List<@Valid Linea> lineas,
    Long clienteId
) {
    public VentaRequest(String numeroFactura, LocalDate fechaVenta,
                        List<Linea> lineas) {
        this(numeroFactura, fechaVenta, lineas, null);
    }

    public record Linea(
        @NotNull @Positive Long productoId,
        @NotNull @Positive Integer cantidad
    ) {}
}
