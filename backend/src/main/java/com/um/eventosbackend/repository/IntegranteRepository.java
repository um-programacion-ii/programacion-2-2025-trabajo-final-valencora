package com.um.eventosbackend.repository;

import com.um.eventosbackend.domain.Integrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link Integrante} entity.
 */
@Repository
public interface IntegranteRepository extends JpaRepository<Integrante, Long> {}

