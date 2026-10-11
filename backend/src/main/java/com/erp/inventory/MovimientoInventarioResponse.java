package com.erp.inventory;

import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Long id,
        Long productoId,
        String sku,
        String nombreProducto,
        TipoMovimiento tipo,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockNuevo,
        String motivo,
        String registradoPor,
        LocalDateTime creadoEn
) {
    public static MovimientoInventarioResponse from(MovimientoInventario movimiento) {
        return new MovimientoInventarioResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getSku(),
                movimiento.getProducto().getNombre(),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getStockAnterior(),
                movimiento.getStockNuevo(),
                movimiento.getMotivo(),
                movimiento.getRegistradoPor(),
                movimiento.getCreadoEn()
        );
    }
}
