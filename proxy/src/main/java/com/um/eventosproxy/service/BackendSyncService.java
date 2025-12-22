package com.um.eventosproxy.service;

import com.um.eventosproxy.config.ProxyProperties;
import com.um.eventosproxy.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio para sincronizar eventos con el backend cuando se reciben mensajes de Kafka.
 */
@Service
public class BackendSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(BackendSyncService.class);

    private final RestTemplate restTemplate;
    private final ProxyProperties proxyProperties;
    private final JwtService jwtService;

    public BackendSyncService(
        RestTemplate restTemplate,
        ProxyProperties proxyProperties,
        JwtService jwtService
    ) {
        this.restTemplate = restTemplate;
        this.proxyProperties = proxyProperties;
        this.jwtService = jwtService;
    }

    /**
     * Sincroniza eventos con el backend llamando al endpoint de sincronización.
     * Este método se llama cuando se recibe un mensaje de Kafka indicando cambios en eventos.
     */
    public void syncEventsWithBackend() {
        String url = proxyProperties.getBackend().getBaseUrl() + proxyProperties.getBackend().getSyncEventsPath();
        LOG.info("🔄 Notificando a backend para sincronizar eventos: {}", url);

        try {
            // Generar token JWT para autenticación con el backend
            String token = jwtService.generateToken("proxy-service");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Realizar la llamada POST al backend para sincronizar eventos
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.info("✅ Sync eventos en backend OK. status={}, body={}", 
                    response.getStatusCode(), response.getBody());
            } else {
                LOG.warn("⚠️ Backend respondió con código no exitoso: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            LOG.error("❌ Error llamando al backend para sync eventos", e);
        }
    }
}

