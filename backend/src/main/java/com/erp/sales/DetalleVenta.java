package com.erp.sales;

import com.erp.products.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_ventas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_detalle_venta_producto",
        columnNames = {"venta_id", "producto_id"}))
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected DetalleVenta() {}

    public DetalleVenta(Venta venta, Producto producto, Integer cantidad,
                        BigDecimal precioUnitario, BigDecimal subtotal) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public Long getProductoId() { return producto.getId(); }
    public String getProductoNombre() { return producto.getNombre(); }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}
