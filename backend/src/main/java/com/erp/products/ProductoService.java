package com.erp.products;

import com.erp.inventory.MovimientoInventario;
import com.erp.inventory.MovimientoInventarioRepository;
import com.erp.inventory.TipoMovimiento;
import com.erp.categories.Categoria;
import com.erp.categories.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;

    @Autowired
    public ProductoService(ProductoRepository repository,
            MovimientoInventarioRepository movimientoRepository,
            CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Constructor compatible con pruebas y usos anteriores sin categorías.
    public ProductoService(ProductoRepository repository,
            MovimientoInventarioRepository movimientoRepository) {
        this(repository, movimientoRepository, null);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return repository.findAllByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request, String registradoPor) {
        if (registradoPor == null || registradoPor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String sku = normalizarSku(request.sku());

        if (repository.existsBySku(sku)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El SKU ya existe");
        }

        Producto producto = new Producto(
                sku,
                request.nombre().trim(),
                limpiarDescripcion(request.descripcion()),
                request.precioCompra(),
                request.precioVenta(),
                request.stockActual(),
                request.stockMinimo()
        );

        producto.asignarCategoria(resolverCategoria(request.categoriaId()));
        Producto guardado = repository.save(producto);

        if (guardado.getStockActual() > 0) {
            movimientoRepository.save(new MovimientoInventario(
                    guardado,
                    TipoMovimiento.AJUSTE,
                    guardado.getStockActual(),
                    0,
                    guardado.getStockActual(),
                    "Stock inicial al crear producto",
                    registradoPor.trim()
            ));
        }

        return ProductoResponse.from(guardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        String sku = normalizarSku(request.sku());

        if (repository.existsBySkuAndIdNot(sku, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El SKU ya pertenece a otro producto");
        }

        producto.actualizar(
                sku,
                request.nombre().trim(),
                limpiarDescripcion(request.descripcion()),
                request.precioCompra(),
                request.precioVenta(),
                producto.getStockActual(),
                request.stockMinimo()
        );
        producto.asignarCategoria(resolverCategoria(request.categoriaId()));

        return ProductoResponse.from(repository.save(producto));
    }

    @Transactional
    public ProductoResponse desactivar(Long id) {
        Producto producto = buscar(id);
        producto.desactivar();
        return ProductoResponse.from(repository.save(producto));
    }

    private Producto buscar(Long id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        if (categoriaRepository == null) {
            throw new IllegalStateException(
                    "CategoriaRepository no está configurado");
        }
        return categoriaRepository.findById(categoriaId)
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Categoría no encontrada o inactiva: " + categoriaId));
    }

    private String normalizarSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }
}
