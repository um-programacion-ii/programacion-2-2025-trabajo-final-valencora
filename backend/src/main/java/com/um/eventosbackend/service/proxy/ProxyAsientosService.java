package com.um.eventosbackend.service.proxy;

import com.um.eventosbackend.config.ApplicationProperties;
import com.um.eventosbackend.service.dto.BloqueoAsientosRequestDTO;
import com.um.eventosbackend.service.dto.BloqueoAsientosResponseDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxyAsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyAsientosService.class);
    private static final String ENDPOINT_MAPA_ASIENTOS = "/api/asientos/evento/";
    private static final String ENDPOINT_BLOQUEO_ASIENTOS = "/api/asientos/bloquear";

    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;

    public ProxyAsientosService(
        @org.springframework.beans.factory.annotation.Qualifier("proxyRestTemplate") RestTemplate restTemplate,
        ApplicationProperties applicationProperties
    ) {
        this.restTemplate = restTemplate;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Obtiene el mapa de asientos de un evento desde el proxy.
     */
    public MapaAsientosDTO obtenerMapaAsientos(Long eventoId) {
        String url = applicationProperties.getProxy().getBaseUrl() + ENDPOINT_MAPA_ASIENTOS + eventoId;
        LOG.debug("Obteniendo mapa de asientos desde proxy: {}", url);

        try {
            ResponseEntity<MapaAsientosDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                MapaAsientosDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LOG.debug("Mapa de asientos obtenido exitosamente para eventoId: {}", eventoId);
                return response.getBody();
            } else {
                LOG.warn("Respuesta vacía o no exitosa del proxy para eventoId: {}", eventoId);
                return crearMapaVacio(eventoId);
            }
        } catch (Exception e) {
            LOG.error("Error al obtener mapa de asientos desde proxy para eventoId: {}", eventoId, e);
            return crearMapaVacio(eventoId);
        }
    }

    /**
     * Bloquea temporalmente los asientos seleccionados.
     */
    public BloqueoAsientosResponseDTO bloquearAsientos(BloqueoAsientosRequestDTO request) {
        String url = applicationProperties.getProxy().getBaseUrl() + ENDPOINT_BLOQUEO_ASIENTOS;
        LOG.debug("Enviando solicitud de bloqueo de asientos al proxy: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<BloqueoAsientosRequestDTO> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<BloqueoAsientosResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                BloqueoAsientosResponseDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LOG.info("Bloqueo de asientos procesado: exitoso={}", response.getBody().getExitoso());
                return response.getBody();
            } else {
                LOG.warn("Respuesta vacía o no exitosa del proxy para bloqueo de asientos");
                return crearRespuestaError("Error al comunicarse con el proxy");
            }
        } catch (Exception e) {
            LOG.error("Error al bloquear asientos a través del proxy", e);
            return crearRespuestaError("Error al comunicarse con el proxy: " + e.getMessage());
        }
    }

    private MapaAsientosDTO crearMapaVacio(Long eventoId) {
        MapaAsientosDTO mapa = new MapaAsientosDTO();
        mapa.setEventoId(eventoId);
        mapa.setAsientos(new java.util.ArrayList<>());
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

