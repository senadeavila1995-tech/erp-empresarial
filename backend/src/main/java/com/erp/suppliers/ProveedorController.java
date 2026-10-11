package com.erp.suppliers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class ProveedorController {
    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProveedorResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorResponse crear(@Valid @RequestBody ProveedorRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ProveedorResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }
}
