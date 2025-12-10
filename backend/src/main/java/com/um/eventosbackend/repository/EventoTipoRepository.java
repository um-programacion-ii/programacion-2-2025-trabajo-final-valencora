package com.um.eventosbackend.repository;

import com.um.eventosbackend.domain.EventoTipo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link EventoTipo} entity.
 */
@Repository
public interface EventoTipoRepository extends JpaRepository<EventoTipo, Long> {
    Optional<EventoTipo> findByNombre(String nombre);
}

