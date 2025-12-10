package com.um.eventosproxy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.dto.AsientoDTO;
import com.um.eventosproxy.dto.MapaAsientosDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(RedisAsientosService.class);
    private static final String REDIS_KEY_PREFIX = "evento:asientos:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAsientosService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public MapaAsientosDTO obtenerMapaAsientos(Long eventoId) {
        String key = REDIS_KEY_PREFIX + eventoId;
        LOG.debug("Consultando mapa de asientos desde Redis para eventoId: {}", eventoId);

        try {
            String data = redisTemplate.opsForValue().get(key);
            if (data == null || data.isEmpty()) {
                LOG.warn("No se encontraron datos de asientos en Redis para eventoId: {}", eventoId);
                return crearMapaVacio(eventoId);
            }

            // Parsear datos de Redis (asumiendo formato JSON o estructura clave-valor)
            Map<String, Object> rawData = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
            List<AsientoDTO> asientos = parsearAsientos(rawData);

            MapaAsientosDTO mapa = new MapaAsientosDTO();
            mapa.setEventoId(eventoId);
            mapa.setAsientos(asientos);

            LOG.debug("Mapa de asientos obtenido: {} asientos para eventoId: {}", asientos.size(), eventoId);
            return mapa;

        } catch (Exception e) {
            LOG.error("Error al obtener mapa de asientos desde Redis para eventoId: {}", eventoId, e);
            return crearMapaVacio(eventoId);
        }
    }

    private List<AsientoDTO> parsearAsientos(Map<String, Object> rawData) {
        List<AsientoDTO> asientos = new ArrayList<>();

        // Intentar parsear diferentes formatos posibles de Redis
        if (rawData.containsKey("asientos")) {
            Object asientosObj = rawData.get("asientos");
            if (asientosObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> asientosList = (List<Map<String, Object>>) asientosObj;
                for (Map<String, Object> asientoData : asientosList) {
                    AsientoDTO asiento = parsearAsiento(asientoData);
                    if (asiento != null) {
                        asientos.add(asiento);
                    }
                }
            }
        } else {
            // Si no hay estructura "asientos", intentar parsear directamente
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> asientoData = (Map<String, Object>) entry.getValue();
                    AsientoDTO asiento = parsearAsiento(asientoData);
                    if (asiento != null) {
                        asientos.add(asiento);
                    }
                }
            }
        }

        return asientos;
    }

    private AsientoDTO parsearAsiento(Map<String, Object> data) {
        try {
            AsientoDTO asiento = new AsientoDTO();
            asiento.setFila((String) data.getOrDefault("fila", ""));
            asiento.setNumero((Integer) data.getOrDefault("numero", 0));

            String estadoStr = (String) data.getOrDefault("estado", "LIBRE");
            try {
                asiento.setEstado(AsientoDTO.EstadoAsiento.valueOf(estadoStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
            }

            return asiento;
        } catch (Exception e) {
            LOG.warn("Error al parsear asiento desde Redis", e);
            return null;
        }
    }

    private MapaAsientosDTO crearMapaVacio(Long eventoId) {
        MapaAsientosDTO mapa = new MapaAsientosDTO();
        mapa.setEventoId(eventoId);
        mapa.setAsientos(new ArrayList<>());
        return mapa;
    }
}

