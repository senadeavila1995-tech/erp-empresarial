package com.erp.suppliers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByIdentificacion(String identificacion);
    boolean existsByIdentificacionAndIdNot(String identificacion, Long id);
    List<Proveedor> findAllByActivoTrueOrderByNombreAsc();
}
