package com.um.eventosbackend.repository;

import com.um.eventosbackend.domain.AsientoVenta;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AsientoVenta entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AsientoVentaRepository extends JpaRepository<AsientoVenta, Long> {
}

