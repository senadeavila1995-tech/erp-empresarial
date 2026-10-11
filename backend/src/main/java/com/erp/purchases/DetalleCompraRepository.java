package com.erp.purchases;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    List<DetalleCompra> findAllByCompraIdOrderByIdAsc(Long compraId);
}
