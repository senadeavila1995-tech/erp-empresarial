package com.erp.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 100)
    private String numeroFactura;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "registrado_por", nullable = false, length = 80)
    private String registradoPor;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Venta() {}

    public Venta(String numeroFactura, LocalDate fechaVenta,
                 BigDecimal subtotal, BigDecimal total, String registradoPor) {
        this.numeroFactura = numeroFactura;
        this.fechaVenta = fechaVenta;
        this.subtotal = subtotal;
        this.total = total;
        this.estado = "CONFIRMADA";
        this.registradoPor = registradoPor;
    }

    public Long getId() { return id; }
    public String getNumeroFactura() { return numeroFactura; }
    public LocalDate getFechaVenta() { return fechaVenta; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTotal() { return total; }
    public String getEstado() { return estado; }
    public String getRegistradoPor() { return registradoPor; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
