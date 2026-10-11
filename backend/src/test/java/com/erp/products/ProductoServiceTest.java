package com.erp.products;

import com.erp.inventory.MovimientoInventario;
import com.erp.inventory.MovimientoInventarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoService(repository, movimientoRepository);
    }

    private ProductoRequest request(
            String sku, String nombre, String descripcion) {
        return new ProductoRequest(
                sku, nombre, descripcion,
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                10, 2
        );
    }

    private Producto producto(Long id, String sku) {
        Producto producto = new Producto(
                sku, "Producto original", "Descripción original",
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"), 10, 2
        );

        // Asignamos el ID mediante reflexión para simular una entidad persistida.
        try {
            var field = Producto.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(producto, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        return producto;
    }

    @Test
    void crearNormalizaSkuYTexto() {
        when(repository.existsBySku("ABC-001")).thenReturn(false);
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse response = service.crear(
                request("  abc-001  ", "  Teclado  ", "  Mecánico  "), "admin-prueba"
        );

        assertEquals("ABC-001", response.sku());
        assertEquals("Teclado", response.nombre());
        assertEquals("Mecánico", response.descripcion());
        assertTrue(response.activo());

        verify(repository).save(any(Producto.class));
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void crearConStockInicialRegistraAjusteConUsuarioAutenticado() {
        when(repository.existsBySku("ABC-010")).thenReturn(false);
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.crear(request("ABC-010", "Teclado", null), "admin-prueba");

        var captor = org.mockito.ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());

        MovimientoInventario movimiento = captor.getValue();
        assertEquals(com.erp.inventory.TipoMovimiento.AJUSTE, movimiento.getTipo());
        assertEquals(10, movimiento.getCantidad());
        assertEquals(0, movimiento.getStockAnterior());
        assertEquals(10, movimiento.getStockNuevo());
        assertEquals("admin-prueba", movimiento.getRegistradoPor());
        assertEquals("Stock inicial al crear producto", movimiento.getMotivo());
    }

    @Test
    void crearConStockInicialCeroNoRegistraMovimiento() {
        ProductoRequest sinStock = new ProductoRequest(
                "ABC-011", "Teclado", null,
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"), 0, 2
        );

        when(repository.existsBySku("ABC-011")).thenReturn(false);
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.crear(sinStock, "admin-prueba");

        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void crearConSkuDuplicadoDevuelve409() {
        when(repository.existsBySku("ABC-001")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.crear(request("abc-001", "Teclado", null), "admin-prueba")
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(Producto.class));
        verifyNoInteractions(movimientoRepository);
    }

    @Test
    void crearDescripcionEnBlancoLaConvierteEnNull() {
        when(repository.existsBySku("ABC-002")).thenReturn(false);
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse response = service.crear(
                request("ABC-002", "Ratón", "   "), "admin-prueba"
        );

        assertNull(response.descripcion());
    }

    @Test
    void actualizarConSkuDeOtroProductoDevuelve409() {
        Producto existente = producto(1L, "ABC-001");

        when(repository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(existente));
        when(repository.existsBySkuAndIdNot("ABC-002", 1L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.actualizar(
                        1L, request("ABC-002", "Actualizado", null))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any(Producto.class));
    }

    @Test
    void actualizarProductoActualizaSusDatos() {
        Producto existente = producto(1L, "ABC-001");

        when(repository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(existente));
        when(repository.existsBySkuAndIdNot("ABC-001", 1L))
                .thenReturn(false);
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductoResponse response = service.actualizar(
                1L, request("abc-001", "  Monitor  ", null)
        );

        assertEquals("ABC-001", response.sku());
        assertEquals("Monitor", response.nombre());
        assertNull(response.descripcion());
        verify(repository).save(existente);
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() {
        when(repository.findByIdAndActivoTrue(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.obtener(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void desactivarCambiaEstadoSinEliminarRegistro() {
        Producto existente = producto(1L, "ABC-001");

        when(repository.findByIdAndActivoTrue(1L))
                .thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        ProductoResponse response = service.desactivar(1L);

        assertFalse(response.activo());
        verify(repository).save(existente);
        verify(repository, never()).delete(any(Producto.class));
    }

    @Test
    void listarDevuelveProductosActivosEnElOrdenDelRepositorio() {
        when(repository.findAllByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(
                        producto(1L, "ABC-001"),
                        producto(2L, "ABC-002")
                ));

        List<ProductoResponse> resultado = service.listar();

        assertEquals(2, resultado.size());
        assertEquals("ABC-001", resultado.get(0).sku());
        assertEquals("ABC-002", resultado.get(1).sku());
        verify(repository).findAllByActivoTrueOrderByNombreAsc();
    }
}
