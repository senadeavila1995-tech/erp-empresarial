package com.erp.sales;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(
            @Valid @RequestBody VentaRequest request,
            Authentication authentication) {
        return service.registrar(request, authentication.getName());
    }
}
