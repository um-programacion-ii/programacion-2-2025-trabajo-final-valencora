package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.security.SecurityUtils;
import com.um.eventosbackend.service.asientos.AsientosService;
import com.um.eventosbackend.service.dto.BloqueoAsientosResponseDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asientos")
@PreAuthorize("isAuthenticated()")
public class AsientosResource {

    private static final Logger LOG = LoggerFactory.getLogger(AsientosResource.class);

    private final AsientosService asientosService;

    public AsientosResource(AsientosService asientosService) {
        this.asientosService = asientosService;
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<MapaAsientosDTO> obtenerMapaAsientos(@PathVariable Long eventoId) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para obtener mapa de asientos: eventoId={}, userId={}", eventoId, userId);
        MapaAsientosDTO mapa = asientosService.obtenerMapaAsientos(eventoId, userId);
        return ResponseEntity.ok(mapa);
    }

    @PostMapping("/bloquear/{eventoId}")
    public ResponseEntity<BloqueoAsientosResponseDTO> bloquearAsientos(@PathVariable Long eventoId) {
        String userId = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para bloquear asientos: eventoId={}, userId={}", eventoId, userId);
        BloqueoAsientosResponseDTO resultado = asientosService.bloquearAsientos(eventoId, userId);
        return ResponseEntity.ok(resultado);
    }
}

