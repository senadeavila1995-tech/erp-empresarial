package com.erp.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario>
    findAllByProductoIdOrderByCreadoEnDescIdDesc(Long productoId);
}
