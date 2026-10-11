package com.erp.products;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(
            @Valid @RequestBody ProductoRequest request,
            Authentication authentication) {
        ProductoResponse producto = service.crear(request, authentication.getName());
        return ResponseEntity
                .created(URI.create("/api/products/" + producto.id()))
                .body(producto);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductoResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }
}
