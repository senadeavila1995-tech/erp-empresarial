package com.erp.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank @Size(max = 50)
        String sku,

        @NotBlank @Size(max = 150)
        String nombre,

        @Size(max = 1000)
        String descripcion,

        @NotNull @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal precioCompra,

        @NotNull @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal precioVenta,

        @NotNull @Min(0)
        Integer stockActual,

        @NotNull @Min(0)
        Integer stockMinimo,

        Long categoriaId
) {
    public ProductoRequest(
            String sku, String nombre, String descripcion,
            BigDecimal precioCompra, BigDecimal precioVenta,
            Integer stockActual, Integer stockMinimo) {
        this(sku, nombre, descripcion, precioCompra, precioVenta,
                stockActual, stockMinimo, null);
    }
}
