package com.um.eventosproxy.service.catedra;

import com.um.eventosproxy.config.ProxyProperties;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class CatedraTokenInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraTokenInterceptor.class);

    private final ProxyProperties proxyProperties;

    public CatedraTokenInterceptor(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String token = proxyProperties.getCatedra().getAuthToken();
        if (token == null || token.isBlank()) {
            LOG.warn("No hay token de cátedra configurado en el proxy");
            throw new IllegalStateException("No hay token de cátedra configurado");
        }

        HttpHeaders headers = request.getHeaders();
        headers.setBearerAuth(token.trim());
        ClientHttpResponse response = execution.execute(request, body);

        var statusCode = response.getStatusCode();
        if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            LOG.warn("La cátedra rechazó el token configurado en el proxy");
            throw new RuntimeException("Token inválido o expirado para el servicio de la cátedra");
        }
        return response;
    }
}

