package com.erp.inventory;

import com.erp.products.Producto;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "stock_nuevo", nullable = false)
    private Integer stockNuevo;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "registrado_por", nullable = false, length = 80)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(
            Producto producto,
            TipoMovimiento tipo,
            Integer cantidad,
            Integer stockAnterior,
            Integer stockNuevo,
            String motivo,
            String registradoPor) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
    }

    public Long getId() { return id; }
    public Producto getProducto() { return producto; }
    public TipoMovimiento getTipo() { return tipo; }
    public Integer getCantidad() { return cantidad; }
    public Integer getStockAnterior() { return stockAnterior; }
    public Integer getStockNuevo() { return stockNuevo; }
    public String getMotivo() { return motivo; }
    public String getRegistradoPor() { return registradoPor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
