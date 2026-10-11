package com.erp.sales;

import com.erp.inventory.MovimientoInventario;
import com.erp.inventory.MovimientoInventarioRepository;
import com.erp.products.Producto;
import com.erp.products.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VentaServiceTest {

    private final VentaRepository ventas = mock(VentaRepository.class);
    private final DetalleVentaRepository detalles = mock(DetalleVentaRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final MovimientoInventarioRepository movimientos =
        mock(MovimientoInventarioRepository.class);

    private final VentaService service =
        new VentaService(ventas, detalles, productos, movimientos);

    private Producto producto(Long id, String nombre, String precio, int stock) {
        Producto p = mock(Producto.class);
        when(p.getId()).thenReturn(id);
        when(p.getNombre()).thenReturn(nombre);
        when(p.getPrecioVenta()).thenReturn(new BigDecimal(precio));
        when(p.getStockActual()).thenReturn(stock);
        return p;
    }

    private VentaRequest request(String factura, Long productoId, int cantidad) {
        return new VentaRequest(
            factura,
            LocalDate.now(),
            List.of(new VentaRequest.Linea(productoId, cantidad))
        );
    }

    @Test
    void rechazaFacturaDuplicada() {
        when(ventas.existsByNumeroFactura("FAC-001")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request(" FAC-001 ", 1L, 1), "admin")
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(productos, detalles, movimientos);
    }

    @Test
    void rechazaProductoRepetidoEnLineas() {
        when(ventas.existsByNumeroFactura("FAC-002")).thenReturn(false);

        VentaRequest request = new VentaRequest(
            "FAC-002",
            LocalDate.now(),
            List.of(
                new VentaRequest.Linea(1L, 1),
                new VentaRequest.Linea(1L, 2)
            )
        );

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request, "admin")
        );

        assertEquals(400, ex.getStatusCode().value());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(productos, detalles, movimientos);
    }

    @Test
    void rechazaStockInsuficienteSinCrearVenta() {
        Producto p = producto(1L, "Teclado", "50000.00", 2);
        when(ventas.existsByNumeroFactura("FAC-003")).thenReturn(false);
        when(productos.findActivoByIdForUpdate(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request("FAC-003", 1L, 3), "vendedor")
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(ventas, never()).saveAndFlush(any());
        verify(productos, never()).save(any());
        verifyNoInteractions(detalles, movimientos);
    }

    @Test
    void rechazaUsuarioVacio() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request("FAC-004", 1L, 1), " ")
        );

        assertEquals(401, ex.getStatusCode().value());
        verifyNoInteractions(ventas, detalles, productos, movimientos);
    }

    @Test
    void rechazaProductoInexistenteOInactivo() {
        when(ventas.existsByNumeroFactura("FAC-005")).thenReturn(false);
        when(productos.findActivoByIdForUpdate(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request("FAC-005", 99L, 1), "vendedor")
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(detalles, movimientos);
    }


    @Test
    void calculaTotalYDescuentaStockAlRegistrarVenta() {
        Producto p = producto(1L, "Teclado", "50000.00", 10);
        when(ventas.existsByNumeroFactura("FAC-006")).thenReturn(false);
        when(productos.findActivoByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(ventas.saveAndFlush(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            try {
                var field = Venta.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(v, 101L);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return v;
        });
        when(detalles.save(any(DetalleVenta.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientos.save(any(MovimientoInventario.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(ventas.findById(101L)).thenAnswer(invocation ->
            Optional.of(new Venta("FAC-006", LocalDate.now(),
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), "admin") {
            }));

        // Se usa un detalle vacío al consultar porque este test verifica
        // el cálculo y los efectos de escritura, no la carga de respuesta.
        when(detalles.findAllByVentaIdOrderByIdAsc(101L)).thenReturn(List.of());

        VentaResponse response = service.registrar(
            new VentaRequest("FAC-006", LocalDate.now(),
                List.of(new VentaRequest.Linea(1L, 2))), "admin");

        assertEquals(new BigDecimal("100000.00"), response.total());
        verify(p).actualizarStock(8);
        verify(productos).save(p);
        verify(ventas).saveAndFlush(any(Venta.class));
        verify(detalles).save(any(DetalleVenta.class));
        verify(movimientos).save(any(MovimientoInventario.class));
    }

    @Test
    void noGuardaDetallesNiMovimientosSiFallaAntesDeCrearLaVenta() {
        Producto p = producto(1L, "Teclado", "50000.00", 1);
        when(ventas.existsByNumeroFactura("FAC-007")).thenReturn(false);
        when(productos.findActivoByIdForUpdate(1L)).thenReturn(Optional.of(p));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request("FAC-007", 1L, 2), "admin"));

        assertEquals(409, ex.getStatusCode().value());
        verify(ventas, never()).saveAndFlush(any());
        verify(productos, never()).save(any());
        verifyNoInteractions(detalles, movimientos);
    }

}
