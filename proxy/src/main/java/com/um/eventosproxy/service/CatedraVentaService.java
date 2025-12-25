package com.um.eventosproxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.config.ProxyProperties;
import com.um.eventosproxy.dto.BloqueoAsientosRequestDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio para comunicarse con el servicio de la cátedra para confirmar ventas.
 */
@Service
public class CatedraVentaService {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraVentaService.class);
    private static final String ENDPOINT_CONFIRMAR_VENTA = "/api/endpoints/v1/realizar-venta";

    private final RestTemplate catedraRestTemplate;
    private final ProxyProperties proxyProperties;
    private final ObjectMapper objectMapper;

    public CatedraVentaService(
        @org.springframework.beans.factory.annotation.Qualifier("catedraRestTemplate") RestTemplate catedraRestTemplate,
        ProxyProperties proxyProperties,
        ObjectMapper objectMapper
    ) {
        this.catedraRestTemplate = catedraRestTemplate;
        this.proxyProperties = proxyProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Confirma una venta con el servicio de la cátedra.
     */
    public Map<String, Object> confirmarVenta(BloqueoAsientosRequestDTO request, List<Map<String, String>> nombresPersonas, Double precioVenta) {
        LOG.info("=== CONFIRMANDO VENTA === eventoId={}, cantidad asientos={}", 
            request.getEventoId(), 
            request.getAsientos() != null ? request.getAsientos().size() : 0);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Preparar el request para la cátedra 
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("eventoId", request.getEventoId());
            requestBody.put("fecha", java.time.Instant.now().toString());
            // precioVenta viene del backend o se calcula como 0.0 si no se proporciona
            requestBody.put("precioVenta", precioVenta != null ? precioVenta : 0.0);
            
            List<Map<String, Object>> asientosConNombres = new ArrayList<>();
            for (int i = 0; i < request.getAsientos().size(); i++) {
                BloqueoAsientosRequestDTO.AsientoBloqueoDTO asiento = request.getAsientos().get(i);
                Map<String, Object> asientoConNombre = new HashMap<>();
                asientoConNombre.put("fila", asiento.getFila());
                asientoConNombre.put("columna", asiento.getColumna());
                
                if (i < nombresPersonas.size()) {
                    Map<String, String> nombrePersona = nombresPersonas.get(i);
                    String nombreCompleto = nombrePersona.get("nombre") + " " + nombrePersona.get("apellido");
                    asientoConNombre.put("persona", nombreCompleto);
                }
                
                asientosConNombres.add(asientoConNombre);
            }
            requestBody.put("asientos", asientosConNombres);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            LOG.info("Request JSON que se enviará a la cátedra: {}", requestJson);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);
            
            LOG.info("Solicitando confirmación de venta en endpoint: {}", ENDPOINT_CONFIRMAR_VENTA);

            ResponseEntity<String> rawResponse = catedraRestTemplate.exchange(
                ENDPOINT_CONFIRMAR_VENTA,
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
                    Map<String, Object> responseMap = objectMapper.readValue(
                        rawResponse.getBody(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                    
                    LOG.info("Venta confirmada exitosamente por la cátedra: {}", responseMap);
                    return responseMap;
                } catch (Exception parseException) {
                    LOG.error("Error al parsear respuesta de confirmación de venta: {}", rawResponse.getBody(), parseException);
                    return crearRespuestaError("Error al parsear respuesta del servicio de la cátedra: " + parseException.getMessage());
                }
            } else {
                LOG.warn("Respuesta vacía o no exitosa de la cátedra para confirmar venta: status={}", rawResponse.getStatusCode());
                return crearRespuestaError("Error al comunicarse con el servicio de la cátedra");
            }
        } catch (RestClientException e) {
            LOG.error("Error al confirmar venta en el servicio de la cátedra", e);
            return crearRespuestaError("Error al comunicarse con el servicio de la cátedra: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error inesperado al confirmar venta", e);
            return crearRespuestaError("Error inesperado: " + e.getMessage());
        }
    }

    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> error = new HashMap<>();
        error.put("resultado", "FALLIDA");
        error.put("mensaje", mensaje);
        return error;
    }
}

