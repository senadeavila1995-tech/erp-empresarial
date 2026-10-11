package com.erp.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    List<DetalleVenta> findAllByVentaIdOrderByIdAsc(Long ventaId);
}
