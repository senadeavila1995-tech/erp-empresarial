package com.erp.customers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @Size(max = 20) String tipoIdentificacion,
    @NotBlank @Size(max = 50) String identificacion,
    @NotBlank @Size(max = 180) String nombre,
    @Size(max = 40) String telefono,
    @Email @Size(max = 180) String email,
    @Size(max = 300) String direccion
) {}
