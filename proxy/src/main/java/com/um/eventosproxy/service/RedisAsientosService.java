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
    // Formato real de las claves en Redis según la cátedra y el enunciado: "evento_{id}"
    // El valor es un JSON con "eventoId" y "asientos"
    private static final String REDIS_KEY_PREFIX = "evento_";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CatedraAsientosService catedraAsientosService;

    public RedisAsientosService(
        RedisTemplate<String, String> redisTemplate, 
        ObjectMapper objectMapper,
        CatedraAsientosService catedraAsientosService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.catedraAsientosService = catedraAsientosService;
    }

    public MapaAsientosDTO obtenerMapaAsientos(Long eventoId) {
        // La cátedra y la guía de la materia usan claves del tipo "evento_{id}"
        String key = REDIS_KEY_PREFIX + eventoId;
        LOG.info("=== CONSULTANDO ASIENTOS === eventoId: {}, buscando key en Redis: '{}'", eventoId, key);

        try {
            String data = redisTemplate.opsForValue().get(key);
            LOG.info("Resultado de Redis GET para key '{}': data es null={}, vacío={}, longitud={}", 
                key, data == null, data != null && data.isEmpty(), data != null ? data.length() : 0);
            
            if (data == null || data.isEmpty()) {
                LOG.warn("No se encontraron datos de asientos en Redis para eventoId: {}, key: {}", eventoId, key);
                // Si no hay datos en Redis simplemente devolvemos un mapa vacío.
                // La cátedra solo guarda asientos cuando hay bloqueos; si no existen, no hay nada que mostrar.
                return crearMapaVacio(eventoId);
            }

            LOG.info("Datos obtenidos de Redis (primeros 1000 caracteres): {}", 
                data.length() > 1000 ? data.substring(0, 1000) + "..." : data);

            // Intentar parsear como objeto JSON primero
            List<AsientoDTO> asientos = new ArrayList<>();
            try {
                Object parsed = objectMapper.readValue(data, Object.class);
                LOG.info("Tipo de dato parseado de Redis: {}", parsed.getClass().getSimpleName());
                
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawData = (Map<String, Object>) parsed;
                    LOG.info("Datos parseados de Redis como Map: keys={}", rawData.keySet());
                    
                    // Intentar diferentes estructuras posibles
                    if (rawData.containsKey("asientos")) {
                        Object asientosObj = rawData.get("asientos");
                        if (asientosObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> asientosList = (List<Object>) asientosObj;
                            LOG.info("Encontrado campo 'asientos' con {} elementos", asientosList.size());
                            for (Object item : asientosList) {
                                if (item instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> asientoData = (Map<String, Object>) item;
                                    LOG.info("Procesando asiento desde Redis (dentro de Map.asientos): {}", asientoData);
                                    AsientoDTO asiento = parsearAsiento(asientoData);
                                    if (asiento != null) {
                                        asientos.add(asiento);
                                    }
                                }
                            }
                        }
                    } else {
                        // Si no hay campo "asientos", intentar parsear el Map directamente como si fuera un array de asientos
                        asientos = parsearAsientos(rawData);
                    }
                } else if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> asientosList = (List<Object>) parsed;
                    LOG.info("Datos parseados de Redis como List directo: {} elementos", asientosList.size());
                    for (Object item : asientosList) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> asientoData = (Map<String, Object>) item;
                            LOG.info("Procesando asiento desde Redis (List directo): {}", asientoData);
                            AsientoDTO asiento = parsearAsiento(asientoData);
                            if (asiento != null) {
                                asientos.add(asiento);
                            }
                        } else {
                            LOG.warn("Elemento en List no es un Map: {}", item.getClass());
                        }
                    }
                } else {
                    LOG.warn("Formato de datos en Redis no reconocido: {}", parsed.getClass());
                }
            } catch (Exception parseException) {
                LOG.error("Error al parsear datos de Redis: {}", parseException.getMessage(), parseException);
                LOG.error("Datos que causaron el error (primeros 500 chars): {}", 
                    data.length() > 500 ? data.substring(0, 500) + "..." : data);
                throw parseException;
            }
            
            LOG.info("Asientos parseados desde Redis: {} total, {} bloqueados", 
                asientos.size(),
                asientos.stream()
                    .filter(a -> a.getEstado() == AsientoDTO.EstadoAsiento.BLOQUEADO)
                    .count());

            MapaAsientosDTO mapa = new MapaAsientosDTO();
            mapa.setEventoId(eventoId);
            mapa.setAsientos(asientos);

            LOG.debug("Mapa de asientos obtenido: {} asientos para eventoId: {}", asientos.size(), eventoId);
            return mapa;

        } catch (Exception e) {
            LOG.error("Error al obtener mapa de asientos desde Redis para eventoId: {}, key: {}", eventoId, key, e);
            LOG.error("Stack trace completo:", e);
            // Ante cualquier error al leer/parsear Redis, devolvemos un mapa vacío
            // para no romper la consulta de detalle.
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
                List<Object> asientosList = (List<Object>) asientosObj;
                LOG.info("Parseando {} asientos desde campo 'asientos'", asientosList.size());
                for (Object item : asientosList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> asientoData = (Map<String, Object>) item;
                        AsientoDTO asiento = parsearAsiento(asientoData);
                        if (asiento != null) {
                            asientos.add(asiento);
                        }
                    }
                }
            }
        } else {
            // Si no hay estructura "asientos", intentar parsear directamente
            // Puede ser que el Map contenga asientos como valores
            LOG.info("No se encontró campo 'asientos', intentando parsear Map directamente");
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> asientoData = (Map<String, Object>) entry.getValue();
                    AsientoDTO asiento = parsearAsiento(asientoData);
                    if (asiento != null) {
                        asientos.add(asiento);
                    }
                } else if (entry.getValue() instanceof List) {
                    // Puede ser que haya una lista de asientos en algún campo
                    @SuppressWarnings("unchecked")
                    List<Object> lista = (List<Object>) entry.getValue();
                    for (Object item : lista) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> asientoData = (Map<String, Object>) item;
                            AsientoDTO asiento = parsearAsiento(asientoData);
                            if (asiento != null) {
                                asientos.add(asiento);
                            }
                        }
                    }
                }
            }
        }

        return asientos;
    }

    private AsientoDTO parsearAsiento(Map<String, Object> data) {
        try {
            AsientoDTO asiento = new AsientoDTO();
            
            // Manejar fila: puede venir como String o Integer
            Object filaObj = data.getOrDefault("fila", "");
            if (filaObj instanceof Integer) {
                asiento.setFila(String.valueOf(filaObj));
            } else if (filaObj instanceof String) {
                asiento.setFila((String) filaObj);
            } else {
                asiento.setFila("");
            }
            
            // Manejar numero/columna: puede venir como "numero" o "columna"
            Object numeroObj = data.getOrDefault("numero", data.getOrDefault("columna", 0));
            if (numeroObj instanceof Integer) {
                asiento.setNumero((Integer) numeroObj);
            } else if (numeroObj instanceof Number) {
                asiento.setNumero(((Number) numeroObj).intValue());
            } else {
                asiento.setNumero(0);
            }

            // Manejar estado: puede venir como "Bloqueado", "BLOQUEADO", "Libre", "Ocupado", etc.
            Object estadoObj = data.get("estado");
            String estadoStr = null;
            if (estadoObj instanceof String) {
                estadoStr = (String) estadoObj;
            } else if (estadoObj != null) {
                estadoStr = estadoObj.toString();
            }
            
            if (estadoStr != null && !estadoStr.isEmpty()) {
                try {
                    // Normalizar el estado: "Bloqueado" -> "BLOQUEADO", "Libre" -> "LIBRE", "Ocupado" -> "OCUPADO"
                    String estadoNormalizado = estadoStr.toUpperCase().trim();
                    
                    // Mapear variaciones comunes
                    if (estadoNormalizado.equals("BLOQUEADO") || estadoNormalizado.equals("BLOQUEADOS") 
                        || estadoNormalizado.contains("BLOQUEO") || estadoNormalizado.equals("BLOQUEO EXITOSO")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.BLOQUEADO);
                    } else if (estadoNormalizado.equals("OCUPADO") || estadoNormalizado.equals("OCUPADOS") 
                        || estadoNormalizado.equals("VENDIDO") || estadoNormalizado.equals("VENDIDOS")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.OCUPADO);
                    } else if (estadoNormalizado.equals("LIBRE") || estadoNormalizado.equals("LIBRES") 
                        || estadoNormalizado.equals("DISPONIBLE") || estadoNormalizado.equals("DISPONIBLES")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                    } else {
                        // Intentar parsear directamente
                        try {
                            asiento.setEstado(AsientoDTO.EstadoAsiento.valueOf(estadoNormalizado));
                        } catch (IllegalArgumentException e) {
                            LOG.warn("Estado '{}' no reconocido después de todos los intentos, usando LIBRE", estadoStr);
                            asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("Estado '{}' no reconocido después de normalizar, usando LIBRE por defecto", estadoStr);
                    asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                }
            } else {
                asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
            }

            LOG.info("Asiento parseado desde Redis: fila={}, numero={}, estado={}", 
                asiento.getFila(), asiento.getNumero(), asiento.getEstado());
            
            return asiento;
        } catch (Exception e) {
            LOG.warn("Error al parsear asiento desde Redis: {}", data, e);
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
