package com.erp.purchases;

import com.erp.inventory.MovimientoInventario;
import com.erp.inventory.MovimientoInventarioRepository;
import com.erp.inventory.TipoMovimiento;
import com.erp.products.Producto;
import com.erp.products.ProductoRepository;
import com.erp.suppliers.Proveedor;
import com.erp.suppliers.ProveedorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompraService {
    private static final BigDecimal MAX_SUBTOTAL =
        new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_COSTO_UNITARIO =
        new BigDecimal("9999999999.99");

    private final CompraRepository compras;
    private final DetalleCompraRepository detalles;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final MovimientoInventarioRepository movimientos;

    public CompraService(CompraRepository compras, DetalleCompraRepository detalles,
            ProveedorRepository proveedores, ProductoRepository productos,
            MovimientoInventarioRepository movimientos) {
        this.compras = compras;
        this.detalles = detalles;
        this.proveedores = proveedores;
        this.productos = productos;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listar() {
        return compras.findAllByOrderByIdDesc().stream()
            .map(c -> CompraResponse.from(c,
                detalles.findAllByCompraIdOrderByIdAsc(c.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        Compra c = compras.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        return CompraResponse.from(c, detalles.findAllByCompraIdOrderByIdAsc(id));
    }

    @Transactional
    public CompraResponse registrar(CompraRequest r, String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado");
        }

        Proveedor proveedor = proveedores.findById(r.proveedorId())
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Proveedor no encontrado o inactivo"));

        String factura = r.numeroFactura().trim();
        if (compras.existsByProveedorIdAndNumeroFactura(proveedor.getId(), factura)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe una compra con esa factura para este proveedor");
        }

        Set<Long> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraRequest.Linea linea : r.lineas()) {
            if (!ids.add(linea.productoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No repitas un producto en varias líneas de la compra");
            }
            if (linea.costoUnitario().scale() > 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El costo unitario admite máximo dos decimales");
            }
            if (linea.costoUnitario().compareTo(MAX_COSTO_UNITARIO) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El costo unitario supera el máximo permitido");
            }

            BigDecimal importe = linea.costoUnitario()
                .multiply(BigDecimal.valueOf(linea.cantidad()))
                .setScale(2, java.math.RoundingMode.UNNECESSARY);

            if (importe.compareTo(MAX_SUBTOTAL) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subtotal de una línea supera el máximo permitido");
            }

            total = total.add(importe);
            if (total.compareTo(MAX_SUBTOTAL) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El subtotal de la compra supera el máximo permitido");
            }
        }

        Compra compra;
        try {
            compra = compras.saveAndFlush(new Compra(
                proveedor, factura, r.fechaCompra(), total, usuario.trim()));
        } catch (DataIntegrityViolationException ex) {
            if (esFacturaDuplicada(ex)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una compra con esa factura para este proveedor", ex);
            }
            throw ex;
        }

        // Bloquear siempre los productos por ID ascendente para reducir
        // el riesgo de interbloqueos en compras concurrentes.
        List<CompraRequest.Linea> lineasOrdenadas = r.lineas().stream()
            .sorted(java.util.Comparator.comparing(CompraRequest.Linea::productoId))
            .toList();

        for (CompraRequest.Linea linea : lineasOrdenadas) {
            Producto producto = productos.findActivoByIdForUpdate(linea.productoId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Producto no encontrado o inactivo: " + linea.productoId()));

            int anterior = producto.getStockActual();
            int nuevo;
            try {
                nuevo = Math.addExact(anterior, linea.cantidad());
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cantidad excede el límite de inventario");
            }

            BigDecimal importe = linea.costoUnitario()
                .multiply(BigDecimal.valueOf(linea.cantidad()))
                .setScale(2, java.math.RoundingMode.UNNECESSARY);

            producto.actualizarStock(nuevo);
            productos.save(producto);

            detalles.save(new DetalleCompra(
                compra, producto, linea.cantidad(), linea.costoUnitario(), importe));

            movimientos.save(new MovimientoInventario(
                producto, TipoMovimiento.ENTRADA, linea.cantidad(), anterior, nuevo,
                "Compra #" + compra.getId() + " / factura " + factura,
                usuario.trim()));
        }

        return obtener(compra.getId());
    }

    private boolean esFacturaDuplicada(DataIntegrityViolationException ex) {
        Throwable causa = ex;
        while (causa != null) {
            String mensaje = causa.getMessage();
            if (mensaje != null
                    && mensaje.contains("uk_compras_proveedor_factura")) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
