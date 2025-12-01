package com.um.eventosbackend.service.catedra.http;

import com.um.eventosbackend.service.catedra.CatedraTokenService;
import com.um.eventosbackend.service.catedra.exception.CatedraAuthenticationException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class CatedraTokenInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraTokenInterceptor.class);

    private final CatedraTokenService tokenService;

    public CatedraTokenInterceptor(CatedraTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.setBearerAuth(tokenService.requireToken());
        ClientHttpResponse response = execution.execute(request, body);

        var statusCode = response.getStatusCode();
        if (statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            LOG.warn("La cátedra rechazó el token configurado. Solicite uno nuevo e inténtelo otra vez.");
            throw new CatedraAuthenticationException(
                HttpStatus.valueOf(statusCode.value()),
                "Token inválido o expirado para el servicio de la cátedra"
            );
        }
        return response;
    }
}

