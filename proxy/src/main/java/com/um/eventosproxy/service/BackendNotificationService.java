package com.um.eventosproxy.service;

import com.um.eventosproxy.config.ProxyProperties;
import com.um.eventosproxy.dto.NotificacionEventoDTO;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BackendNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(BackendNotificationService.class);
    private static final String NOTIFICATION_ENDPOINT = "/api/admin/eventos/notificacion";

    private final RestTemplate restTemplate;
    private final ProxyProperties proxyProperties;
    private final JwtService jwtService;

    public BackendNotificationService(
        RestTemplate restTemplate,
        ProxyProperties proxyProperties,
        JwtService jwtService
    ) {
        this.restTemplate = restTemplate;
        this.proxyProperties = proxyProperties;
        this.jwtService = jwtService;
    }

    @Async
    public CompletableFuture<Void> notificarCambioEvento(NotificacionEventoDTO notificacion) {
        LOG.info("Notificando cambio de evento al backend: eventoId={}, tipoCambio={}", 
            notificacion.getEventoIdCatedra(), notificacion.getTipoCambio());

        try {
            String url = proxyProperties.getBackend().getBaseUrl() + NOTIFICATION_ENDPOINT;
            String token = jwtService.generateToken("proxy-service");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<NotificacionEventoDTO> request = new HttpEntity<>(notificacion, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.info("Notificación enviada exitosamente al backend");
            } else {
                LOG.warn("Backend respondió con código no exitoso: {}", response.getStatusCode());
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            LOG.error("Error al notificar cambio de evento al backend", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

