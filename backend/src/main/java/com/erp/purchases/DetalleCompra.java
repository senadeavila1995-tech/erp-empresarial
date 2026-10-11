package com.erp.purchases;

import com.erp.products.Producto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_compras",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_detalle_compra_producto",
        columnNames = {"compra_id", "producto_id"}))
public class DetalleCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected DetalleCompra() {}

    public DetalleCompra(Compra compra, Producto producto, Integer cantidad,
                         BigDecimal costoUnitario, BigDecimal subtotal) {
        this.compra = compra;
        this.producto = producto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.subtotal = subtotal;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return producto.getId(); }
    public String getProductoNombre() { return producto.getNombre(); }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}
