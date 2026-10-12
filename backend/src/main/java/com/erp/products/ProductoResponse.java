package com.erp.products;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        Integer stockActual,
        Integer stockMinimo,
        Boolean activo,
        Long categoriaId,
        String categoriaNombre
) {
    public ProductoResponse(
            Long id, String sku, String nombre, String descripcion,
            BigDecimal precioCompra, BigDecimal precioVenta,
            Integer stockActual, Integer stockMinimo, Boolean activo) {
        this(id, sku, nombre, descripcion, precioCompra, precioVenta,
                stockActual, stockMinimo, activo, null, null);
    }

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecioCompra(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getActivo(),
                producto.getCategoria() == null ? null : producto.getCategoria().getId(),
                producto.getCategoria() == null ? null : producto.getCategoria().getNombre()
        );
    }
}
