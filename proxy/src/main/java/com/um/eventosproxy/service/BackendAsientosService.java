package com.um.eventosproxy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.config.ProxyProperties;
import java.util.Map;
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
public class BackendAsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(BackendAsientosService.class);
    private static final String DIMENSIONES_ENDPOINT = "/api/eventos/catedra/{eventoIdCatedra}/dimensiones";

    private final RestTemplate restTemplate;
    private final ProxyProperties proxyProperties;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public BackendAsientosService(
        RestTemplate restTemplate,
        ProxyProperties proxyProperties,
        ObjectMapper objectMapper,
        JwtService jwtService
    ) {
        this.restTemplate = restTemplate;
        this.proxyProperties = proxyProperties;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    /**
     * Obtiene las dimensiones (filas y columnas) de un evento desde el backend.
     */
    public Map<String, Integer> obtenerDimensionesEvento(Long eventoIdCatedra) {
        LOG.info("Obteniendo dimensiones del evento {} desde el backend", eventoIdCatedra);
        try {
            String url = proxyProperties.getBackend().getBaseUrl() + DIMENSIONES_ENDPOINT.replace("{eventoIdCatedra}", String.valueOf(eventoIdCatedra));
            String token = jwtService.generateToken("proxy-service");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    Map<String, Integer> dimensiones = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Integer>>() {}
                    );
                    
                    if (dimensiones != null && dimensiones.containsKey("filas") && dimensiones.containsKey("columnas")) {
                        LOG.info("Dimensiones obtenidas desde el backend: {} filas x {} columnas", 
                            dimensiones.get("filas"), dimensiones.get("columnas"));
                        return dimensiones;
                    } else {
                        LOG.warn("Dimensiones incompletas desde el backend para evento {}: {}", eventoIdCatedra, dimensiones);
                    }
                } catch (Exception e) {
                    LOG.error("Error al parsear dimensiones desde el backend para evento {}: {}", eventoIdCatedra, e.getMessage(), e);
                }
            } else {
                LOG.warn("Backend respondió con código no exitoso para dimensiones del evento {}: {}", 
                    eventoIdCatedra, response.getStatusCode());
            }
        } catch (Exception e) {
            LOG.error("Error al obtener dimensiones del evento {} desde el backend: {}", eventoIdCatedra, e.getMessage(), e);
        }
        return null;
    }
}

