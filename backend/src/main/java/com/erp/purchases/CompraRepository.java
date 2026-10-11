package com.erp.purchases;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);
    List<Compra> findAllByOrderByIdDesc();
}
