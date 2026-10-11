package com.erp.purchases;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class CompraController {
    private final CompraService service;

    public CompraController(CompraService service) {
        this.service = service;
    }

    @GetMapping
    public List<CompraResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse registrar(@Valid @RequestBody CompraRequest request,
                                    Authentication authentication) {
        return service.registrar(request, authentication.getName());
    }
}
