package com.erp.customers;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public java.util.List<ClienteResponse> listar() {
        return repository.findAllByActivoTrueOrderByNombreAsc()
            .stream().map(ClienteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.from(buscarActivo(id));
    }

    public ClienteResponse crear(ClienteRequest request) {
        String identificacion = limpiar(request.identificacion())
            .toUpperCase(Locale.ROOT);

        if (repository.existsByIdentificacionIgnoreCase(identificacion)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un cliente con esa identificación"
            );
        }

        Cliente cliente = new Cliente(
            limpiarOpcional(request.tipoIdentificacion()),
            identificacion,
            limpiar(request.nombre()),
            limpiarOpcional(request.telefono()),
            limpiarOpcional(request.email()),
            limpiarOpcional(request.direccion())
        );

        return ClienteResponse.from(repository.save(cliente));
    }

    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarActivo(id);
        String identificacion = limpiar(request.identificacion())
            .toUpperCase(Locale.ROOT);

        if (repository.existsByIdentificacionIgnoreCaseAndIdNot(
                identificacion, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe otro cliente con esa identificación"
            );
        }

        cliente.actualizar(
            limpiarOpcional(request.tipoIdentificacion()),
            identificacion,
            limpiar(request.nombre()),
            limpiarOpcional(request.telefono()),
            limpiarOpcional(request.email()),
            limpiarOpcional(request.direccion())
        );

        return ClienteResponse.from(repository.save(cliente));
    }

    public void desactivar(Long id) {
        Cliente cliente = buscarActivo(id);
        cliente.desactivar();
        repository.save(cliente);
    }

    private Cliente buscarActivo(Long id) {
        Cliente cliente = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Cliente no encontrado"
            ));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Cliente no encontrado"
            );
        }

        return cliente;
    }

    private String limpiar(String valor) {
        return valor.trim();
    }

    private String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
