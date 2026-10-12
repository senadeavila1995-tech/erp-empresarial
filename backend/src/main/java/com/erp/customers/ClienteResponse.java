package com.erp.customers;

public record ClienteResponse(
    Long id,
    String tipoIdentificacion,
    String identificacion,
    String nombre,
    String telefono,
    String email,
    String direccion,
    Boolean activo
) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
            cliente.getId(),
            cliente.getTipoIdentificacion(),
            cliente.getIdentificacion(),
            cliente.getNombre(),
            cliente.getTelefono(),
            cliente.getEmail(),
            cliente.getDireccion(),
            cliente.getActivo()
        );
    }
}
