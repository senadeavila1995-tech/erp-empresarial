package com.erp.customers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findAllByActivoTrueOrderByNombreAsc();
    boolean existsByIdentificacionIgnoreCase(String identificacion);
    boolean existsByIdentificacionIgnoreCaseAndIdNot(
        String identificacion, Long id
    );
}
