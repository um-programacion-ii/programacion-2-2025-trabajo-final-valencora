package com.um.eventosproxy.web.rest;

import com.um.eventosproxy.dto.MapaAsientosDTO;
import com.um.eventosproxy.service.RedisAsientosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asientos")
public class AsientosResource {

    private static final Logger LOG = LoggerFactory.getLogger(AsientosResource.class);

    private final RedisAsientosService redisAsientosService;

    public AsientosResource(RedisAsientosService redisAsientosService) {
        this.redisAsientosService = redisAsientosService;
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<MapaAsientosDTO> obtenerMapaAsientos(@PathVariable Long eventoId) {
        LOG.debug("REST request para obtener mapa de asientos del evento: {}", eventoId);

        MapaAsientosDTO mapa = redisAsientosService.obtenerMapaAsientos(eventoId);
        return ResponseEntity.ok(mapa);
    }
}

