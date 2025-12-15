package com.um.eventosproxy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.config.ProxyProperties;
import com.um.eventosproxy.dto.BloqueoAsientosRequestDTO;
import com.um.eventosproxy.dto.BloqueoAsientosResponseDTO;
import com.um.eventosproxy.dto.MapaAsientosDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio para comunicarse con el servicio de la cátedra para bloqueo de asientos.
 * <p>
 * Usa el endpoint `/api/endpoints/v1/asientos/bloquear` del servicio de la cátedra,
 * siguiendo el mismo patrón que `/api/endpoints/v1/eventos`.
 */
@Service
public class CatedraAsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraAsientosService.class);
    private static final String ENDPOINT_BLOQUEO_ASIENTOS = "/api/endpoints/v1/bloquear-asientos";

    private final RestTemplate catedraRestTemplate;
    private final ProxyProperties proxyProperties;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String ENDPOINT_EVENTO_DETALLE = "/api/endpoints/v1/eventos/{id}";

    public CatedraAsientosService(
        @org.springframework.beans.factory.annotation.Qualifier("catedraRestTemplate") RestTemplate catedraRestTemplate,
        ProxyProperties proxyProperties,
        ObjectMapper objectMapper,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.catedraRestTemplate = catedraRestTemplate;
        this.proxyProperties = proxyProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Bloquea temporalmente los asientos en el servicio de la cátedra.
     * <p>
     * El bloqueo dura 5 minutos según el Issue #15.
     */
    public BloqueoAsientosResponseDTO bloquearAsientos(BloqueoAsientosRequestDTO request) {
        LOG.info("=== BLOQUEANDO ASIENTOS === eventoId={}, cantidad={}", 
            request.getEventoId(), 
            request.getAsientos() != null ? request.getAsientos().size() : 0);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Log del request que se va a enviar
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                LOG.info("Request JSON que se enviará a la cátedra: {}", requestJson);
            } catch (Exception e) {
                LOG.warn("No se pudo serializar el request para logging: {}", e.getMessage());
            }

            HttpEntity<BloqueoAsientosRequestDTO> httpEntity = new HttpEntity<>(request, headers);
            
            LOG.info("Solicitando bloqueo en endpoint: {} (baseUrl ya configurada en RestTemplate)", ENDPOINT_BLOQUEO_ASIENTOS);

            ResponseEntity<String> rawResponse = catedraRestTemplate.exchange(
                ENDPOINT_BLOQUEO_ASIENTOS,
                HttpMethod.POST,
                httpEntity,
                String.class
            );

            LOG.info("Respuesta cruda de la cátedra: status={}, body={}", 
                rawResponse.getStatusCode(), 
                rawResponse.getBody() != null && rawResponse.getBody().length() > 1000 
                    ? rawResponse.getBody().substring(0, 1000) + "..." 
                    : rawResponse.getBody());

            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null) {
                try {
                    BloqueoAsientosResponseDTO responseDTO = objectMapper.readValue(
                        rawResponse.getBody(), 
                        BloqueoAsientosResponseDTO.class
                    );
                    
                    // Convertir el formato de la cátedra al formato esperado
                    // La cátedra devuelve "asientos" con estados, necesitamos separarlos
                    if (responseDTO.getAsientos() != null && !responseDTO.getAsientos().isEmpty()) {
                        List<BloqueoAsientosResponseDTO.AsientoBloqueoDTO> bloqueados = new ArrayList<>();
                        List<BloqueoAsientosResponseDTO.AsientoBloqueoDTO> noDisponibles = new ArrayList<>();
                        
                        for (BloqueoAsientosResponseDTO.AsientoConEstadoDTO asiento : responseDTO.getAsientos()) {
                            BloqueoAsientosResponseDTO.AsientoBloqueoDTO dto = new BloqueoAsientosResponseDTO.AsientoBloqueoDTO();
                            dto.setFila(asiento.getFila());
                            dto.setColumna(asiento.getColumna());
                            
                            // Separar según el estado
                            String estado = asiento.getEstado() != null ? asiento.getEstado().toUpperCase().trim() : "";
                            if (estado.equals("BLOQUEADO") || estado.equals("BLOQUEO EXITOSO") || estado.contains("BLOQUEO")) {
                                bloqueados.add(dto);
                            } else if (estado.equals("OCUPADO") || estado.equals("VENDIDO")) {
                                noDisponibles.add(dto);
                            }
                        }
                        
                        responseDTO.setAsientosBloqueados(bloqueados);
                        responseDTO.setAsientosNoDisponibles(noDisponibles);
                    }
                    
                    // Asegurar que exitoso y mensaje estén poblados (normalizar formato de la cátedra)
                    if (responseDTO.getExitoso() == null && responseDTO.getResultado() != null) {
                        responseDTO.setExitoso(responseDTO.getResultado());
                    }
                    if ((responseDTO.getMensaje() == null || responseDTO.getMensaje().isEmpty()) && responseDTO.getDescripcion() != null) {
                        responseDTO.setMensaje(responseDTO.getDescripcion());
                    }
                    
                    LOG.info("Bloqueo de asientos procesado por la cátedra: exitoso={}, bloqueados={}, no disponibles={}", 
                        responseDTO.obtenerExitoso(),
                        responseDTO.getAsientosBloqueados() != null ? responseDTO.getAsientosBloqueados().size() : 0,
                        responseDTO.getAsientosNoDisponibles() != null ? responseDTO.getAsientosNoDisponibles().size() : 0);

                    // Actualizar nuestro Redis local para que el proxy pueda devolver los asientos bloqueados
                    if (Boolean.TRUE.equals(responseDTO.obtenerExitoso()) 
                        && responseDTO.getAsientosBloqueados() != null 
                        && !responseDTO.getAsientosBloqueados().isEmpty()) {
                        try {
                            LOG.info("Actualizando Redis con {} asientos bloqueados para evento {}", 
                                responseDTO.getAsientosBloqueados().size(), request.getEventoId());
                            actualizarBloqueosEnRedis(request.getEventoId(), responseDTO);
                            LOG.info("Redis actualizado exitosamente para evento {}", request.getEventoId());
                        } catch (Exception e) {
                            LOG.error("Error al actualizar Redis con los bloqueos del evento {}: {}", 
                                request.getEventoId(), e.getMessage(), e);
                            // No fallar el bloqueo si Redis falla, solo loguear el error
                        }
                    } else {
                        LOG.info("No se actualiza Redis: bloqueo exitoso={}, asientos bloqueados={}", 
                            responseDTO.obtenerExitoso(),
                            responseDTO.getAsientosBloqueados() != null ? responseDTO.getAsientosBloqueados().size() : 0);
                    }
                    return responseDTO;
                } catch (Exception parseException) {
                    LOG.error("Error al parsear respuesta de bloqueo de asientos: {}", rawResponse.getBody(), parseException);
                    return crearRespuestaError("Error al parsear respuesta del servicio de la cátedra: " + parseException.getMessage());
                }
            } else {
                LOG.warn("Respuesta vacía o no exitosa de la cátedra para bloqueo de asientos: status={}", rawResponse.getStatusCode());
                return crearRespuestaError("Error al comunicarse con el servicio de la cátedra");
            }
        } catch (RestClientException e) {
            LOG.error("Error al bloquear asientos en el servicio de la cátedra", e);
            return crearRespuestaError("Error al comunicarse con el servicio de la cátedra: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error inesperado al bloquear asientos", e);
            return crearRespuestaError("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Obtiene el mapa de asientos desde el servicio de la cátedra consultando el detalle del evento.
     * Este método se usa como fallback cuando Redis está vacío.
     */
    public MapaAsientosDTO obtenerMapaAsientosDesdeCatedra(Long eventoId) {
        LOG.warn("=== FALLBACK: Redis no tiene datos. El endpoint de detalle individual no existe en la cátedra. ===");
        LOG.warn("=== SOLUCIÓN: Los asientos bloqueados solo están disponibles en Redis después de bloquear asientos. ===");
        LOG.warn("=== Ejecuta el endpoint POST /api/asientos/bloquear para generar los datos en Redis. ===");
        return crearMapaVacio(eventoId);
    }

    /**
     * Actualiza la estructura de Redis para un evento dado a partir de la respuesta de Cátedra.
     * Formato en Redis (clave: "evento_{id}"):
     * {
     *   "eventoId": 4,
     *   "asientos": [
     *     { "fila": 2, "columna": 3, "estado": "Bloqueado", "expira": "2025-12-15T14:00:00Z" }
     *   ]
     * }
     */
    private void actualizarBloqueosEnRedis(Long eventoId, BloqueoAsientosResponseDTO responseDTO) throws Exception {
        final String key = "evento_" + eventoId;
        LOG.info("Actualizando Redis para los bloqueos de evento {} en key '{}'", eventoId, key);

        // Leer estado actual
        String jsonActual = redisTemplate.opsForValue().get(key);
        Map<String, Object> estadoActual;
        if (jsonActual != null && !jsonActual.isBlank()) {
            estadoActual = objectMapper.readValue(
                jsonActual,
                new TypeReference<Map<String, Object>>() {}
            );
        } else {
            estadoActual = new java.util.HashMap<>();
            estadoActual.put("eventoId", eventoId);
            estadoActual.put("asientos", new java.util.ArrayList<Map<String, Object>>());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> asientosActuales =
            (List<Map<String, Object>>) estadoActual.get("asientos");
        if (asientosActuales == null) {
            estadoActual.put("asientos", new java.util.ArrayList<Map<String, Object>>());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listaAsientos = (List<Map<String, Object>>) estadoActual.get("asientos");

        // Procesar asientos bloqueados exitosamente
        if (responseDTO.getAsientosBloqueados() != null && !responseDTO.getAsientosBloqueados().isEmpty()) {
            for (BloqueoAsientosResponseDTO.AsientoBloqueoDTO bloqueado : responseDTO.getAsientosBloqueados()) {
                if (bloqueado.getFila() == null || bloqueado.getColumna() == null) {
                    continue;
                }
                int fila = bloqueado.getFila();
                int columna = bloqueado.getColumna();

                Map<String, Object> asientoRedis = null;
                for (Map<String, Object> a : listaAsientos) {
                    int f = ((Number) a.getOrDefault("fila", -1)).intValue();
                    int c = ((Number) a.getOrDefault("columna", -1)).intValue();
                    if (f == fila && c == columna) {
                        asientoRedis = a;
                        break;
                    }
                }

                if (asientoRedis == null) {
                    asientoRedis = new java.util.HashMap<>();
                    asientoRedis.put("fila", fila);
                    asientoRedis.put("columna", columna);
                    listaAsientos.add(asientoRedis);
                }

                // Guardar estado como "Bloqueado" y una expiración de 5 minutos
                asientoRedis.put("estado", "Bloqueado");
                asientoRedis.put("expira", Instant.now().plus(Duration.ofMinutes(5)).toString());
            }
        }
        
        // También guardar los asientos que NO se pudieron bloquear (ocupados/no disponibles)
        // para tener información completa en Redis
        if (responseDTO.getAsientosNoDisponibles() != null && !responseDTO.getAsientosNoDisponibles().isEmpty()) {
            for (BloqueoAsientosResponseDTO.AsientoBloqueoDTO noDisponible : responseDTO.getAsientosNoDisponibles()) {
                if (noDisponible.getFila() == null || noDisponible.getColumna() == null) {
                    continue;
                }
                int fila = noDisponible.getFila();
                int columna = noDisponible.getColumna();

                // Verificar si ya existe
                Map<String, Object> asientoRedis = null;
                for (Map<String, Object> a : listaAsientos) {
                    int f = ((Number) a.getOrDefault("fila", -1)).intValue();
                    int c = ((Number) a.getOrDefault("columna", -1)).intValue();
                    if (f == fila && c == columna) {
                        asientoRedis = a;
                        break;
                    }
                }

                // Solo agregar si no existe (no sobrescribir un asiento bloqueado)
                if (asientoRedis == null) {
                    asientoRedis = new java.util.HashMap<>();
                    asientoRedis.put("fila", fila);
                    asientoRedis.put("columna", columna);
                    asientoRedis.put("estado", "Ocupado");
                    listaAsientos.add(asientoRedis);
                }
            }
        }
        
        // También procesar los asientos que vienen en el campo "asientos" de la respuesta
        // que pueden tener información adicional sobre el estado
        if (responseDTO.getAsientos() != null && !responseDTO.getAsientos().isEmpty()) {
            for (BloqueoAsientosResponseDTO.AsientoConEstadoDTO asientoConEstado : responseDTO.getAsientos()) {
                if (asientoConEstado.getFila() == null || asientoConEstado.getColumna() == null) {
                    continue;
                }
                int fila = asientoConEstado.getFila();
                int columna = asientoConEstado.getColumna();
                String estado = asientoConEstado.getEstado();

                // Buscar si ya existe
                Map<String, Object> asientoRedis = null;
                for (Map<String, Object> a : listaAsientos) {
                    int f = ((Number) a.getOrDefault("fila", -1)).intValue();
                    int c = ((Number) a.getOrDefault("columna", -1)).intValue();
                    if (f == fila && c == columna) {
                        asientoRedis = a;
                        break;
                    }
                }

                if (asientoRedis == null) {
                    asientoRedis = new java.util.HashMap<>();
                    asientoRedis.put("fila", fila);
                    asientoRedis.put("columna", columna);
                    listaAsientos.add(asientoRedis);
                }

                // Actualizar estado según lo que dice la cátedra
                if (estado != null && !estado.isEmpty()) {
                    // Normalizar el estado
                    String estadoNormalizado = estado.toUpperCase().trim();
                    if (estadoNormalizado.contains("BLOQUEO") || estadoNormalizado.equals("BLOQUEADO")) {
                        asientoRedis.put("estado", "Bloqueado");
                        asientoRedis.put("expira", Instant.now().plus(Duration.ofMinutes(5)).toString());
                    } else if (estadoNormalizado.equals("OCUPADO") || estadoNormalizado.equals("VENDIDO")) {
                        asientoRedis.put("estado", "Ocupado");
                    } else {
                        asientoRedis.put("estado", "Libre");
                    }
                }
            }
        }

        String jsonNuevo = objectMapper.writeValueAsString(estadoActual);
        redisTemplate.opsForValue().set(key, jsonNuevo);
        LOG.info("Redis actualizado para evento {}: {} asientos", eventoId, listaAsientos.size());
    }

    private List<com.um.eventosproxy.dto.AsientoDTO> convertirAsientosCatedra(List<Map<String, Object>> asientosCatedra) {
        List<com.um.eventosproxy.dto.AsientoDTO> asientos = new ArrayList<>();
        
        for (Map<String, Object> asientoData : asientosCatedra) {
            com.um.eventosproxy.dto.AsientoDTO asiento = new com.um.eventosproxy.dto.AsientoDTO();
            
            // Convertir fila: la cátedra devuelve número, necesitamos string
            Object filaObj = asientoData.get("fila");
            if (filaObj instanceof Integer) {
                asiento.setFila(String.valueOf(filaObj));
            } else if (filaObj instanceof Number) {
                asiento.setFila(String.valueOf(((Number) filaObj).intValue()));
            } else if (filaObj instanceof String) {
                asiento.setFila((String) filaObj);
            } else {
                continue; // Saltar si no hay fila válida
            }
            
            // Convertir columna a numero: la cátedra usa "columna", nosotros "numero"
            Object columnaObj = asientoData.get("columna");
            if (columnaObj instanceof Integer) {
                asiento.setNumero((Integer) columnaObj);
            } else if (columnaObj instanceof Number) {
                asiento.setNumero(((Number) columnaObj).intValue());
            } else {
                continue; // Saltar si no hay columna válida
            }
            
            // Convertir estado: la cátedra devuelve "Bloqueado" (con mayúscula inicial), necesitamos enum
            String estadoStr = (String) asientoData.get("estado");
            if (estadoStr != null && !estadoStr.isEmpty()) {
                try {
                    String estadoNormalizado = estadoStr.toUpperCase().trim();
                    asiento.setEstado(com.um.eventosproxy.dto.AsientoDTO.EstadoAsiento.valueOf(estadoNormalizado));
                } catch (IllegalArgumentException e) {
                    LOG.warn("Estado '{}' no reconocido, usando LIBRE por defecto", estadoStr);
                    asiento.setEstado(com.um.eventosproxy.dto.AsientoDTO.EstadoAsiento.LIBRE);
                }
            } else {
                asiento.setEstado(com.um.eventosproxy.dto.AsientoDTO.EstadoAsiento.LIBRE);
            }
            
            asientos.add(asiento);
        }
        
        return asientos;
    }
    
    private MapaAsientosDTO crearMapaVacio(Long eventoId) {
        MapaAsientosDTO mapa = new MapaAsientosDTO();
        mapa.setEventoId(eventoId);
        mapa.setAsientos(new ArrayList<>());
        return mapa;
    }

    private BloqueoAsientosResponseDTO crearRespuestaError(String mensaje) {
        BloqueoAsientosResponseDTO response = new BloqueoAsientosResponseDTO();
        response.setExitoso(false);
        response.setMensaje(mensaje);
        response.setAsientosBloqueados(new java.util.ArrayList<>());
        response.setAsientosNoDisponibles(new java.util.ArrayList<>());
        return response;
    }
}

