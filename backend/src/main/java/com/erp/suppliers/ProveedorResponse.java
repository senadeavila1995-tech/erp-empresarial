package com.erp.suppliers;

public record ProveedorResponse(
    Long id, String tipoIdentificacion, String identificacion,
    String nombre, String contacto, String telefono, String email,
    String direccion, Boolean activo
) {
    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(
            p.getId(), p.getTipoIdentificacion(), p.getIdentificacion(),
            p.getNombre(), p.getContacto(), p.getTelefono(),
            p.getEmail(), p.getDireccion(), p.getActivo());
    }
}
