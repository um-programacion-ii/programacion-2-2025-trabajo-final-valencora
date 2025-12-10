package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.service.EventoQueryService;
import com.um.eventosbackend.service.dto.EventoDetalleDTO;
import com.um.eventosbackend.service.dto.EventoResumenDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller para leer eventos (listado y detalle).
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventoResource.class);

    private final EventoQueryService eventoQueryService;

    public EventoResource(EventoQueryService eventoQueryService) {
        this.eventoQueryService = eventoQueryService;
    }

    /**
     * {@code GET /api/eventos} : listado de eventos activos (no cancelados y no expirados).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventoResumenDTO>> listarEventos() {
        LOG.debug("REST request to get active events");
        List<EventoResumenDTO> eventos = eventoQueryService.obtenerEventosActivos();
        return ResponseEntity.ok(eventos);
    }

    /**
     * {@code GET /api/eventos/:id} : detalle de un evento activo (no cancelado y no expirado).
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventoDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        LOG.debug("REST request to get event detail : {}", id);
        return eventoQueryService.obtenerDetalleEvento(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

