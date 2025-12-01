package com.um.eventosbackend.service.catedra;

import com.um.eventosbackend.config.ApplicationProperties;
import com.um.eventosbackend.service.catedra.exception.CatedraAuthenticationException;
import com.um.eventosbackend.service.catedra.exception.MissingCatedraTokenException;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mantiene y expone el token entregado por la cátedra.
 */
@Service
public class CatedraTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraTokenService.class);

    private final ApplicationProperties applicationProperties;
    private final AtomicReference<String> currentToken = new AtomicReference<>();

    public CatedraTokenService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    void init() {
        String configuredToken = applicationProperties.getCatedra().getAuthToken();
        if (configuredToken != null && !configuredToken.isBlank()) {
            currentToken.set(configuredToken.trim());
            LOG.info("Token inicial de la cátedra cargado desde configuración");
        } else {
            LOG.warn("No se detectó token de la cátedra en la configuración. Las llamadas externas fallarán hasta registrarlo");
        }
    }

    public Optional<String> getToken() {
        return Optional.ofNullable(currentToken.get());
    }

    public String requireToken() {
        return getToken().orElseThrow(MissingCatedraTokenException::new);
    }

    public void updateToken(String newToken) {
        if (newToken == null || newToken.isBlank()) {
            throw new CatedraAuthenticationException("El token recibido es vacío");
        }
        String sanitized = newToken.trim();
        if (!Objects.equals(currentToken.get(), sanitized)) {
            currentToken.set(sanitized);
            LOG.info("Token de la cátedra actualizado en memoria");
        } else {
            LOG.info("El token de la cátedra recibido coincide con el actual. No se realizaron cambios");
        }
    }

    public void clearToken() {
        currentToken.set(null);
        LOG.info("Token de la cátedra eliminado de la memoria del backend");
    }
}

