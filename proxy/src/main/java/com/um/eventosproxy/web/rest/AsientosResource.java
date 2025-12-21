package com.um.eventosproxy.web.rest;

import com.um.eventosproxy.dto.BloqueoAsientosRequestDTO;
import com.um.eventosproxy.dto.BloqueoAsientosResponseDTO;
import com.um.eventosproxy.dto.MapaAsientosDTO;
import com.um.eventosproxy.service.CatedraAsientosService;
import com.um.eventosproxy.service.RedisAsientosService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asientos")
public class AsientosResource {

    private static final Logger LOG = LoggerFactory.getLogger(AsientosResource.class);

    private final RedisAsientosService redisAsientosService;
    private final CatedraAsientosService catedraAsientosService;

    public AsientosResource(
        RedisAsientosService redisAsientosService,
        CatedraAsientosService catedraAsientosService
    ) {
        this.redisAsientosService = redisAsientosService;
        this.catedraAsientosService = catedraAsientosService;
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<MapaAsientosDTO> obtenerMapaAsientos(@PathVariable Long eventoId) {
        LOG.debug("REST request para obtener mapa de asientos del evento: {}", eventoId);

        MapaAsientosDTO mapa = redisAsientosService.obtenerMapaAsientos(eventoId);
        
        // Solo devolver la matriz, no incluir los asientos individuales
        MapaAsientosDTO respuesta = new MapaAsientosDTO();
        respuesta.setEventoId(mapa.getEventoId());
        respuesta.setMatriz(mapa.getMatriz() != null ? mapa.getMatriz() : new java.util.ArrayList<>());
        respuesta.setAsientos(null); // null para que no se incluya en el JSON
        
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/bloquear")
    public ResponseEntity<BloqueoAsientosResponseDTO> bloquearAsientos(@Valid @RequestBody BloqueoAsientosRequestDTO request) {
        LOG.debug("REST request para bloquear asientos: eventoId={}, cantidad={}", 
            request.getEventoId(), 
            request.getAsientos() != null ? request.getAsientos().size() : 0);

        BloqueoAsientosResponseDTO resultado = catedraAsientosService.bloquearAsientos(request);
        return ResponseEntity.ok(resultado);
    }
}

