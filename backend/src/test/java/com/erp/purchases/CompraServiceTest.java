package com.erp.purchases;

import com.erp.inventory.MovimientoInventarioRepository;
import com.erp.products.ProductoRepository;
import com.erp.suppliers.Proveedor;
import com.erp.suppliers.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompraServiceTest {

    private CompraService crearServicio(
            CompraRepository compras,
            ProveedorRepository proveedores) {
        return new CompraService(
            compras,
            mock(DetalleCompraRepository.class),
            proveedores,
            mock(ProductoRepository.class),
            mock(MovimientoInventarioRepository.class));
    }

    @Test
    void rechazaFacturaYaRegistrada() {
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Proveedor proveedor = mock(Proveedor.class);

        when(proveedor.getId()).thenReturn(1L);
        when(proveedor.getActivo()).thenReturn(true);
        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor));
        when(compras.existsByProveedorIdAndNumeroFactura(1L, "FAC-001"))
            .thenReturn(true);

        CompraService service = crearServicio(compras, proveedores);
        CompraRequest request = new CompraRequest(
            1L, "FAC-001", LocalDate.now(),
            List.of(new CompraRequest.Linea(
                10L, 2, new java.math.BigDecimal("100.00"))));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request, "admin"));

        assertEquals(409, ex.getStatusCode().value());
        verify(compras, never()).saveAndFlush(any());
    }

    @Test
    void rechazaProductoRepetidoEnLaCompra() {
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Proveedor proveedor = mock(Proveedor.class);

        when(proveedor.getId()).thenReturn(1L);
        when(proveedor.getActivo()).thenReturn(true);
        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor));
        when(compras.existsByProveedorIdAndNumeroFactura(1L, "FAC-002"))
            .thenReturn(false);

        CompraService service = crearServicio(compras, proveedores);
        CompraRequest request = new CompraRequest(
            1L, "FAC-002", LocalDate.now(),
            List.of(
                new CompraRequest.Linea(10L, 1,
                    new java.math.BigDecimal("100.00")),
                new CompraRequest.Linea(10L, 2,
                    new java.math.BigDecimal("100.00"))));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request, "admin"));

        assertEquals(400, ex.getStatusCode().value());
        verify(compras, never()).saveAndFlush(any());
    }

    @Test
    void rechazaCostoUnitarioFueraDeRango() {
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Proveedor proveedor = mock(Proveedor.class);

        when(proveedor.getId()).thenReturn(1L);
        when(proveedor.getActivo()).thenReturn(true);
        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor));

        CompraService service = crearServicio(compras, proveedores);
        CompraRequest request = new CompraRequest(
            1L, "FAC-LIMITE-001", LocalDate.now(),
            List.of(new CompraRequest.Linea(
                10L, 1, new java.math.BigDecimal("10000000000.00"))));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request, "admin"));

        assertEquals(400, ex.getStatusCode().value());
        verify(compras, never()).saveAndFlush(any());
    }

    @Test
    void rechazaSubtotalDeLineaFueraDeRango() {
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Proveedor proveedor = mock(Proveedor.class);

        when(proveedor.getId()).thenReturn(1L);
        when(proveedor.getActivo()).thenReturn(true);
        when(proveedores.findById(1L)).thenReturn(Optional.of(proveedor));

        CompraService service = crearServicio(compras, proveedores);
        CompraRequest request = new CompraRequest(
            1L, "FAC-LIMITE-002", LocalDate.now(),
            List.of(new CompraRequest.Linea(
                10L, 101, new java.math.BigDecimal("9999999999.99"))));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(request, "admin"));

        assertEquals(400, ex.getStatusCode().value());
        verify(compras, never()).saveAndFlush(any());
    }
}
