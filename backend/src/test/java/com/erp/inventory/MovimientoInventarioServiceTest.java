package com.erp.inventory;

import com.erp.products.Producto;
import com.erp.products.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    private MovimientoInventarioService service;

    @BeforeEach
    void setUp() {
        service = new MovimientoInventarioService(
                productoRepository, movimientoRepository);
    }

    private Producto producto(Long id, int stock) {
        Producto producto = new Producto(
                "TEST-INV-" + id,
                "Producto de inventario",
                null,
                new BigDecimal("100.00"),
                new BigDecimal("150.00"),
                stock,
                2
        );

        try {
            var field = Producto.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(producto, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        return producto;
    }

    private MovimientoInventarioRequest request(
            TipoMovimiento tipo, int cantidad, String motivo) {
        return new MovimientoInventarioRequest(tipo, cantidad, motivo);
    }

    @Test
    void entradaIncrementaStockYGuardaMovimiento() {
        Producto producto = producto(1L, 10);

        when(productoRepository.findActivoByIdForUpdate(1L))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioResponse response = service.registrar(
                1L, request(TipoMovimiento.ENTRADA, 5, "Compra recibida"),
                "admin");

        assertEquals(15, producto.getStockActual());
        assertEquals(10, response.stockAnterior());
        assertEquals(15, response.stockNuevo());
        assertEquals(TipoMovimiento.ENTRADA, response.tipo());
        assertEquals("admin", response.registradoPor());

        verify(productoRepository).save(producto);
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void salidaReduceStock() {
        Producto producto = producto(1L, 10);

        when(productoRepository.findActivoByIdForUpdate(1L))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioResponse response = service.registrar(
                1L, request(TipoMovimiento.SALIDA, 4, "Salida por venta"),
                "admin");

        assertEquals(6, producto.getStockActual());
        assertEquals(10, response.stockAnterior());
        assertEquals(6, response.stockNuevo());
    }

    @Test
    void salidaMayorAlStockDevuelve409SinGuardar() {
        Producto producto = producto(1L, 3);

        when(productoRepository.findActivoByIdForUpdate(1L))
                .thenReturn(Optional.of(producto));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(
                        1L,
                        request(TipoMovimiento.SALIDA, 4, "Salida excesiva"),
                        "admin")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(3, producto.getStockActual());
        verify(productoRepository, never()).save(any(Producto.class));
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void ajusteEstableceStockFinalIncluyendoCero() {
        Producto producto = producto(1L, 10);

        when(productoRepository.findActivoByIdForUpdate(1L))
                .thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventarioResponse response = service.registrar(
                1L, request(TipoMovimiento.AJUSTE, 0, "Conteo físico"),
                "admin");

        assertEquals(0, producto.getStockActual());
        assertEquals(10, response.stockAnterior());
        assertEquals(0, response.stockNuevo());
    }

    @Test
    void productoInexistenteDevuelve404() {
        when(productoRepository.findActivoByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(
                        99L,
                        request(TipoMovimiento.ENTRADA, 1, "Prueba"),
                        "admin")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void cantidadCeroEnEntradaEsRechazada() {
        Producto producto = producto(1L, 10);

        when(productoRepository.findActivoByIdForUpdate(1L))
                .thenReturn(Optional.of(producto));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(
                        1L,
                        request(TipoMovimiento.ENTRADA, 0, "Entrada inválida"),
                        "admin")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(10, producto.getStockActual());
        verify(productoRepository, never()).save(any(Producto.class));
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void usuarioVacioEsRechazado() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.registrar(
                        1L,
                        request(TipoMovimiento.ENTRADA, 1, "Prueba"),
                        " ")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(productoRepository, movimientoRepository);
    }
}
