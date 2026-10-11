package com.erp.suppliers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ProveedorService {
    private final ProveedorRepository repository;

    public ProveedorService(ProveedorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return repository.findAllByActivoTrueOrderByNombreAsc()
            .stream().map(ProveedorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.from(buscar(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest r) {
        String identificacion = normalizar(r.identificacion());
        if (repository.existsByIdentificacion(identificacion)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "La identificación del proveedor ya existe");
        }
        Proveedor p = new Proveedor(
            limpiar(r.tipoIdentificacion()), identificacion, r.nombre().trim(),
            limpiar(r.contacto()), limpiar(r.telefono()),
            limpiar(r.email()), limpiar(r.direccion()));
        return ProveedorResponse.from(repository.save(p));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest r) {
        Proveedor p = buscar(id);
        String identificacion = normalizar(r.identificacion());
        if (repository.existsByIdentificacionAndIdNot(identificacion, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "La identificación ya pertenece a otro proveedor");
        }
        p.actualizar(limpiar(r.tipoIdentificacion()), identificacion,
            r.nombre().trim(), limpiar(r.contacto()), limpiar(r.telefono()),
            limpiar(r.email()), limpiar(r.direccion()));
        return ProveedorResponse.from(repository.save(p));
    }

    @Transactional
    public ProveedorResponse desactivar(Long id) {
        Proveedor p = buscar(id);
        p.desactivar();
        return ProveedorResponse.from(repository.save(p));
    }

    private Proveedor buscar(Long id) {
        return repository.findById(id)
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .map(p -> p)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Proveedor no encontrado o inactivo"));
    }

    private String normalizar(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
