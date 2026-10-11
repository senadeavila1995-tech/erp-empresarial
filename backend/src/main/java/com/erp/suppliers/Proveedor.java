package com.erp.suppliers;

import jakarta.persistence.*;

@Entity
@Table(name = "proveedores",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_proveedores_identificacion",
           columnNames = "identificacion"))
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_identificacion", length = 20)
    private String tipoIdentificacion;

    @Column(nullable = false, length = 50)
    private String identificacion;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 120)
    private String contacto;

    @Column(length = 40)
    private String telefono;

    @Column(length = 180)
    private String email;

    @Column(length = 300)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;

    protected Proveedor() {}

    public Proveedor(String tipoIdentificacion, String identificacion,
                     String nombre, String contacto, String telefono,
                     String email, String direccion) {
        this.tipoIdentificacion = tipoIdentificacion;
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.activo = true;
    }

    public void actualizar(String tipoIdentificacion, String identificacion,
                           String nombre, String contacto, String telefono,
                           String email, String direccion) {
        this.tipoIdentificacion = tipoIdentificacion;
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
    }

    public void desactivar() { this.activo = false; }

    public Long getId() { return id; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getContacto() { return contacto; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getDireccion() { return direccion; }
    public Boolean getActivo() { return activo; }
}
