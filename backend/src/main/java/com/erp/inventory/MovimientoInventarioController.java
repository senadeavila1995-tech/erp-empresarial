package com.erp.inventory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productoId}/movements")
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;

    public MovimientoInventarioController(MovimientoInventarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<MovimientoInventarioResponse> listar(
            @PathVariable Long productoId) {
        return service.listar(productoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrar(
            @PathVariable Long productoId,
            @Valid @RequestBody MovimientoInventarioRequest request,
            Authentication authentication) {
        if (request.tipo() == TipoMovimiento.AJUSTE) {
            boolean puedeAjustar = authentication.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_ADMIN")
                            || authority.getAuthority().equals("ROLE_GERENTE"));

            if (!puedeAjustar) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Solo ADMIN o GERENTE pueden ajustar el inventario");
            }
        }

        return service.registrar(
                productoId, request, authentication.getName());
    }
}
