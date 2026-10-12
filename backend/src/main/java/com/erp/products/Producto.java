package com.erp.products;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private com.erp.categories.Categoria categoria;

    protected Producto() {
    }

    public Producto(
            String sku, String nombre, String descripcion,
            BigDecimal precioCompra, BigDecimal precioVenta,
            Integer stockActual, Integer stockMinimo) {
        this.sku = sku;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.activo = true;
    }

    public void actualizar(
            String sku, String nombre, String descripcion,
            BigDecimal precioCompra, BigDecimal precioVenta,
            Integer stockActual, Integer stockMinimo) {
        this.sku = sku;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
    }

    public void actualizarStock(Integer nuevoStock) {
        if (nuevoStock == null || nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.stockActual = nuevoStock;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecioCompra() { return precioCompra; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public Integer getStockActual() { return stockActual; }
    public Integer getStockMinimo() { return stockMinimo; }
    public Boolean getActivo() { return activo; }
    public com.erp.categories.Categoria getCategoria() { return categoria; }
    public void asignarCategoria(com.erp.categories.Categoria categoria) {
        this.categoria = categoria;
    }
}
