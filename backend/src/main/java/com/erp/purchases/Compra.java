package com.erp.purchases;

import com.erp.suppliers.Proveedor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_compras_proveedor_factura",
        columnNames = {"proveedor_id", "numero_factura"}))
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "numero_factura", nullable = false, length = 100)
    private String numeroFactura;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "registrado_por", nullable = false, length = 80)
    private String registradoPor;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Compra() {}

    public Compra(Proveedor proveedor, String numeroFactura,
                  LocalDate fechaCompra, BigDecimal subtotal,
                  String registradoPor) {
        this.proveedor = proveedor;
        this.numeroFactura = numeroFactura;
        this.fechaCompra = fechaCompra;
        this.subtotal = subtotal;
        this.estado = "CONFIRMADA";
        this.registradoPor = registradoPor;
    }

    public Long getId() { return id; }
    public Proveedor getProveedor() { return proveedor; }
    public String getNumeroFactura() { return numeroFactura; }
    public LocalDate getFechaCompra() { return fechaCompra; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getEstado() { return estado; }
    public String getRegistradoPor() { return registradoPor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
