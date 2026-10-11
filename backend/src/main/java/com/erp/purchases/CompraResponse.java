package com.erp.purchases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraResponse(
    Long id, Long proveedorId, String proveedor, String numeroFactura,
    LocalDate fechaCompra, BigDecimal subtotal, String estado,
    String registradoPor, List<Linea> lineas
) {
    public record Linea(Long productoId, String producto, Integer cantidad,
                        BigDecimal costoUnitario, BigDecimal subtotal) {}

    public static CompraResponse from(Compra c, List<DetalleCompra> detalles) {
        return new CompraResponse(
            c.getId(), c.getProveedor().getId(), c.getProveedor().getNombre(),
            c.getNumeroFactura(), c.getFechaCompra(), c.getSubtotal(),
            c.getEstado(), c.getRegistradoPor(),
            detalles.stream().map(d -> new Linea(
                d.getProductoId(), d.getProductoNombre(), d.getCantidad(),
                d.getCostoUnitario(), d.getSubtotal())).toList());
    }
}
