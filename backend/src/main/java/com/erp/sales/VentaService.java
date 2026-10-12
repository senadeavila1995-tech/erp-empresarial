package com.erp.sales;

import com.erp.inventory.MovimientoInventario;
import com.erp.inventory.MovimientoInventarioRepository;
import com.erp.inventory.TipoMovimiento;
import com.erp.customers.Cliente;
import com.erp.customers.ClienteRepository;
import com.erp.products.Producto;
import com.erp.products.ProductoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VentaService {

    private static final BigDecimal MAX_TOTAL =
        new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_PRECIO =
        new BigDecimal("9999999999.99");

    private final VentaRepository ventas;
    private final DetalleVentaRepository detalles;
    private final ProductoRepository productos;
    private final MovimientoInventarioRepository movimientos;
    private final ClienteRepository clientes;

    @Autowired
    public VentaService(
            VentaRepository ventas,
            DetalleVentaRepository detalles,
            ProductoRepository productos,
            MovimientoInventarioRepository movimientos,
            ClienteRepository clientes) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.productos = productos;
        this.movimientos = movimientos;
        this.clientes = clientes;
    }

    // Constructor compatible con pruebas y usos anteriores sin clientes.
    public VentaService(
            VentaRepository ventas,
            DetalleVentaRepository detalles,
            ProductoRepository productos,
            MovimientoInventarioRepository movimientos) {
        this(ventas, detalles, productos, movimientos, null);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventas.findAllByOrderByIdDesc().stream()
            .map(v -> VentaResponse.from(
                v, detalles.findAllByVentaIdOrderByIdAsc(v.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        Venta venta = ventas.findById(id).orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Venta no encontrada"));

        return VentaResponse.from(
            venta, detalles.findAllByVentaIdOrderByIdAsc(id));
    }

    @Transactional
    public VentaResponse registrar(VentaRequest request, String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String factura = request.numeroFactura().trim();

        if (ventas.existsByNumeroFactura(factura)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ya existe una venta con esa factura");
        }

        Set<Long> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (VentaRequest.Linea linea : request.lineas()) {
            if (!ids.add(linea.productoId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No repitas un producto en varias líneas de la venta");
            }
        }

        // Bloquear los productos en orden ascendente de ID.
        List<VentaRequest.Linea> ordenadas = request.lineas().stream()
            .sorted(java.util.Comparator.comparing(
                VentaRequest.Linea::productoId))
            .toList();

        Map<Long, Producto> productosBloqueados = new LinkedHashMap<>();
        Map<Long, BigDecimal> importes = new LinkedHashMap<>();

        for (VentaRequest.Linea linea : ordenadas) {
            Producto producto = productos
                .findActivoByIdForUpdate(linea.productoId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Producto no encontrado o inactivo: "
                        + linea.productoId()));

            if (producto.getStockActual() < linea.cantidad()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock insuficiente para el producto "
                        + producto.getNombre()
                        + ". Disponible: " + producto.getStockActual());
            }

            BigDecimal precio = producto.getPrecioVenta();

            if (precio == null || precio.signum() < 0
                    || precio.scale() > 2
                    || precio.compareTo(MAX_PRECIO) > 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El precio de venta del producto "
                        + producto.getNombre() + " no es válido");
            }

            BigDecimal importe = precio
                .multiply(BigDecimal.valueOf(linea.cantidad()))
                .setScale(2, RoundingMode.UNNECESSARY);

            total = total.add(importe);

            if (importe.compareTo(MAX_TOTAL) > 0
                    || total.compareTo(MAX_TOTAL) > 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El total de la venta supera el máximo permitido");
            }

            productosBloqueados.put(producto.getId(), producto);
            importes.put(producto.getId(), importe);
        }

        Cliente cliente = resolverCliente(request.clienteId());

        Venta venta;
        try {
            Venta nuevaVenta = new Venta(
                factura, request.fechaVenta(), total, total, usuario.trim());
            nuevaVenta.asignarCliente(cliente);
            venta = ventas.saveAndFlush(nuevaVenta);
        } catch (DataIntegrityViolationException ex) {
            if (esFacturaDuplicada(ex)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una venta con esa factura", ex);
            }
            throw ex;
        }

        for (VentaRequest.Linea linea : ordenadas) {
            Producto producto = productosBloqueados.get(linea.productoId());
            int anterior = producto.getStockActual();
            int nuevo = anterior - linea.cantidad();

            producto.actualizarStock(nuevo);
            productos.save(producto);

            detalles.save(new DetalleVenta(
                venta,
                producto,
                linea.cantidad(),
                producto.getPrecioVenta(),
                importes.get(producto.getId())
            ));

            movimientos.save(new MovimientoInventario(
                producto,
                TipoMovimiento.SALIDA,
                linea.cantidad(),
                anterior,
                nuevo,
                "Venta #" + venta.getId() + " / factura " + factura,
                usuario.trim()
            ));
        }

        return obtener(venta.getId());
    }

    private Cliente resolverCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        if (clientes == null) {
            throw new IllegalStateException(
                    "ClienteRepository no está configurado");
        }
        return clientes.findById(clienteId)
            .filter(c -> Boolean.TRUE.equals(c.getActivo()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cliente no encontrado o inactivo: " + clienteId));
    }

    private boolean esFacturaDuplicada(DataIntegrityViolationException ex) {
        Throwable causa = ex;
        while (causa != null) {
            String mensaje = causa.getMessage();
            if (mensaje != null
                    && (mensaje.contains("numero_factura")
                    || mensaje.contains("uk_ventas_numero_factura"))) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
