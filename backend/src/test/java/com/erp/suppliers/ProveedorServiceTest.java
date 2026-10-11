package com.erp.suppliers;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProveedorServiceTest {

    private ProveedorRequest solicitud(String identificacion, String nombre) {
        return new ProveedorRequest(
            "NIT", identificacion, nombre,
            null, null, null, null
        );
    }

    @Test
    void rechazaIdentificacionDuplicada() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.existsByIdentificacion("900123456")).thenReturn(true);

        ProveedorService service = new ProveedorService(repository);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.crear(solicitud("900123456", "Proveedor prueba"))
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void normalizaIdentificacionAlCrear() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.existsByIdentificacion("900123456")).thenReturn(false);
        when(repository.save(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            return p;
        });

        ProveedorService service = new ProveedorService(repository);
        ProveedorResponse response = service.crear(
            solicitud(" 900123456 ", "  Empresa de prueba  ")
        );

        assertEquals("900123456", response.identificacion());
        assertEquals("Empresa de prueba", response.nombre());
        assertEquals(true, response.activo());
        verify(repository).save(any(Proveedor.class));
    }

    @Test
    void listaSoloLosProveedoresActivosDevueltosPorRepositorio() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.findAllByActivoTrueOrderByNombreAsc())
            .thenReturn(List.of());

        ProveedorService service = new ProveedorService(repository);

        assertTrue(service.listar().isEmpty());
        verify(repository).findAllByActivoTrueOrderByNombreAsc();
    }

    @Test
    void obtenerProveedorInexistenteDevuelve404() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ProveedorService service = new ProveedorService(repository);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.obtener(99L)
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void rechazaActualizarProveedorInexistente() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ProveedorService service = new ProveedorService(repository);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.actualizar(99L, solicitud("900123456", "Empresa"))
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaIdentificacionQuePerteneceAOtroProveedor() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        Proveedor proveedor = new Proveedor(
            "NIT", "900111111", "Proveedor existente",
            null, null, null, null
        );

        when(repository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(repository.existsByIdentificacionAndIdNot("900222222", 1L))
            .thenReturn(true);

        ProveedorService service = new ProveedorService(repository);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.actualizar(1L, solicitud("900222222", "Otro nombre"))
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void desactivarCambiaEstadoYGuarda() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        Proveedor proveedor = new Proveedor(
            "NIT", "900123456", "Proveedor activo",
            null, null, null, null
        );

        when(repository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(repository.save(any(Proveedor.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ProveedorService service = new ProveedorService(repository);
        ProveedorResponse response = service.desactivar(1L);

        assertEquals(false, response.activo());
        verify(repository).save(proveedor);
    }

    @Test
    void rechazaDesactivarProveedorInexistente() {
        ProveedorRepository repository = mock(ProveedorRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ProveedorService service = new ProveedorService(repository);
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.desactivar(99L)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }
}
