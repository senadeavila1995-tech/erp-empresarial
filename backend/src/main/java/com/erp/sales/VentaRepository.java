package com.erp.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    boolean existsByNumeroFactura(String numeroFactura);
    List<Venta> findAllByOrderByIdDesc();
    long countByEstadoAndFechaVentaBetween(
        String estado, LocalDate desde, LocalDate hasta);
}
