package com.erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VentaResponse(
    Long id,
    String numeroFactura,
    LocalDate fechaVenta,
    BigDecimal subtotal,
    BigDecimal total,
    String estado,
    String registradoPor,
    List<Linea> lineas,
    Long clienteId,
    String clienteNombre
) {
    public VentaResponse(Long id, String numeroFactura, LocalDate fechaVenta,
            BigDecimal subtotal, BigDecimal total, String estado,
            String registradoPor, List<Linea> lineas) {
        this(id, numeroFactura, fechaVenta, subtotal, total, estado,
                registradoPor, lineas, null, null);
    }

    public record Linea(
        Long productoId,
        String producto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
    ) {}

    public static VentaResponse from(Venta venta, List<DetalleVenta> detalles) {
        return new VentaResponse(
            venta.getId(),
            venta.getNumeroFactura(),
            venta.getFechaVenta(),
            venta.getSubtotal(),
            venta.getTotal(),
            venta.getEstado(),
            venta.getRegistradoPor(),
            detalles.stream().map(d -> new Linea(
                d.getProductoId(),
                d.getProductoNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal()
            )).toList(),
            venta.getCliente() == null ? null : venta.getCliente().getId(),
            venta.getCliente() == null ? null : venta.getCliente().getNombre()
        );
    }
}
