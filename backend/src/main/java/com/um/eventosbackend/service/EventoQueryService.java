package com.um.eventosbackend.service;

import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.service.dto.EventoDetalleDTO;
import com.um.eventosbackend.service.dto.EventoResumenDTO;
import com.um.eventosbackend.service.mapper.EventoMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de consulta de eventos (listado y detalle).
 */
@Service
@Transactional(readOnly = true)
public class EventoQueryService {

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;

    public EventoQueryService(EventoRepository eventoRepository, EventoMapper eventoMapper) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
    }

    /**
     * Obtiene el listado de eventos activos (no cancelados y no expirados).
     */
    public List<EventoResumenDTO> obtenerEventosActivos() {
        Instant ahora = Instant.now();
        List<Evento> eventos = eventoRepository.findEventosActivos(ahora);
        return eventos.stream().map(eventoMapper::toResumenDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle de un evento por ID, si está activo (no cancelado y no expirado).
     */
    public Optional<EventoDetalleDTO> obtenerDetalleEvento(Long id) {
        Instant ahora = Instant.now();
        return eventoRepository
            .findById(id)
            .filter(e -> Boolean.FALSE.equals(e.getCancelado()))
            .filter(e -> e.getFecha() == null || !e.getFecha().isBefore(ahora))
            .map(eventoMapper::toDetalleDTO);
    }
}

