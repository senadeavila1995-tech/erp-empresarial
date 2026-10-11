package com.erp.inventory;

import com.erp.products.Producto;
import com.erp.products.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MovimientoInventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public MovimientoInventarioService(
            ProductoRepository productoRepository,
            MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> listar(Long productoId) {
        if (!productoRepository.findByIdAndActivoTrue(productoId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Producto no encontrado");
        }

        return movimientoRepository
                .findAllByProductoIdOrderByCreadoEnDescIdDesc(productoId)
                .stream()
                .map(MovimientoInventarioResponse::from)
                .toList();
    }

    @Transactional
    public MovimientoInventarioResponse registrar(
            Long productoId,
            MovimientoInventarioRequest request,
            String registradoPor) {

        if (registradoPor == null || registradoPor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        Producto producto = productoRepository
                .findActivoByIdForUpdate(productoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));

        TipoMovimiento tipo = request.tipo();
        int cantidad = request.cantidad();
        int anterior = producto.getStockActual();
        int nuevo;

        switch (tipo) {
            case ENTRADA -> {
                validarCantidadPositiva(cantidad);
                try {
                    nuevo = Math.addExact(anterior, cantidad);
                } catch (ArithmeticException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La cantidad excede el límite permitido");
                }
            }
            case SALIDA -> {
                validarCantidadPositiva(cantidad);
                if (cantidad > anterior) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Stock insuficiente para registrar la salida");
                }
                nuevo = anterior - cantidad;
            }
            case AJUSTE -> nuevo = cantidad;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de movimiento no válido");
        }

        String motivo = request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }

        producto.actualizarStock(nuevo);

        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                tipo,
                cantidad,
                anterior,
                nuevo,
                motivo,
                registradoPor.trim()
        );

        // Ambos cambios se confirman juntos al finalizar la transacción.
        productoRepository.save(producto);
        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        return MovimientoInventarioResponse.from(guardado);
    }

    private void validarCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe ser mayor que cero para entradas y salidas");
        }
    }
}
