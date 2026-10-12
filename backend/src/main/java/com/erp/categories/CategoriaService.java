package com.erp.categories;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoriaService {
    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return repository.findAllByActivoTrueOrderByNombreAsc()
            .stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = normalizar(request.nombre());
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }
        return CategoriaResponse.from(repository.save(
            new Categoria(nombre, limpiar(request.descripcion()))));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = normalizar(request.nombre());

        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "El nombre ya pertenece a otra categoría");
        }

        categoria.actualizar(nombre, limpiar(request.descripcion()));
        return CategoriaResponse.from(repository.save(categoria));
    }

    @Transactional
    public CategoriaResponse desactivar(Long id) {
        Categoria categoria = buscar(id);
        categoria.desactivar();
        return CategoriaResponse.from(repository.save(categoria));
    }

    private Categoria buscar(Long id) {
        return repository.findById(id)
            .filter(c -> Boolean.TRUE.equals(c.getActivo()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Categoría no encontrada o inactiva"));
    }

    private String normalizar(String nombre) {
        return Normalizer.normalize(nombre.trim(), Normalizer.Form.NFC)
            .toUpperCase(Locale.ROOT);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
