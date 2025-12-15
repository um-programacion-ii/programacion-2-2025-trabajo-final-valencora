package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.security.SecurityUtils;
import com.um.eventosbackend.service.dto.EstadoSeleccionDTO;
import com.um.eventosbackend.service.sesion.SesionSeleccionService;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sesion")
@PreAuthorize("isAuthenticated()")
public class SesionSeleccionResource {

    private static final Logger LOG = LoggerFactory.getLogger(SesionSeleccionResource.class);

    private final SesionSeleccionService sesionSeleccionService;

    public SesionSeleccionResource(SesionSeleccionService sesionSeleccionService) {
        this.sesionSeleccionService = sesionSeleccionService;
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoSeleccionDTO> obtenerEstado() {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para obtener estado de selección del usuario: {}", userId);
        EstadoSeleccionDTO estado = sesionSeleccionService.obtenerEstado(userId);
        return ResponseEntity.ok(estado != null ? estado : new EstadoSeleccionDTO());
    }

    @PutMapping("/estado")
    public ResponseEntity<Void> guardarEstado(@Valid @RequestBody EstadoSeleccionDTO estado) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para guardar estado de selección del usuario: {}", userId);
        sesionSeleccionService.guardarEstado(userId, estado);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/evento/{eventoId}")
    public ResponseEntity<Void> actualizarEventoSeleccionado(@PathVariable Long eventoId) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para actualizar evento seleccionado: eventoId={}, userId={}", eventoId, userId);
        sesionSeleccionService.actualizarEventoSeleccionado(userId, eventoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/asientos")
    public ResponseEntity<Void> actualizarAsientosSeleccionados(@Valid @RequestBody EstadoSeleccionDTO.AsientoSeleccionadoDTO[] asientos) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para actualizar asientos seleccionados: userId={}, cantidad={}", userId, asientos.length);
        sesionSeleccionService.actualizarAsientosSeleccionados(userId, asientos);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/nombres")
    public ResponseEntity<Void> actualizarNombresPersonas(@RequestBody Map<String, EstadoSeleccionDTO.AsientoSeleccionadoDTO> nombresPorAsiento) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para actualizar nombres de personas: userId={}", userId);
        sesionSeleccionService.actualizarNombresPersonas(userId, nombresPorAsiento);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/estado")
    public ResponseEntity<Void> limpiarEstado() {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para limpiar estado de selección del usuario: {}", userId);
        sesionSeleccionService.limpiarEstado(userId);
        return ResponseEntity.noContent().build();
    }
}

