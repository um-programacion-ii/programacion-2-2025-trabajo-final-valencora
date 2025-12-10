package com.um.eventosbackend.repository;

import com.um.eventosbackend.domain.Evento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Evento} entity.
 */
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    /**
     * Busca un evento por su ID en el servicio de la cátedra
     */
    Optional<Evento> findByEventoIdCatedra(Long eventoIdCatedra);

    /**
     * Busca eventos no cancelados y no expirados (fecha >= ahora)
     */
    @Query("SELECT e FROM Evento e WHERE e.cancelado = false AND e.fecha >= :ahora ORDER BY e.fecha ASC")
    List<Evento> findEventosActivos(@Param("ahora") Instant ahora);

    /**
     * Busca eventos expirados (fecha < ahora)
     */
    @Query("SELECT e FROM Evento e WHERE e.fecha < :ahora")
    List<Evento> findEventosExpirados(@Param("ahora") Instant ahora);

    /**
     * Busca eventos cancelados
     */
    List<Evento> findByCanceladoTrue();
}

