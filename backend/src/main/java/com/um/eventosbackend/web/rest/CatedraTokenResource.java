package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.config.ApplicationProperties;
import com.um.eventosbackend.service.catedra.CatedraTokenService;
import com.um.eventosbackend.service.dto.CatedraTokenStatusDTO;
import com.um.eventosbackend.service.dto.CatedraTokenUpdateDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catedra-token")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CatedraTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraTokenResource.class);

    private final CatedraTokenService catedraTokenService;
    private final ApplicationProperties applicationProperties;

    public CatedraTokenResource(CatedraTokenService catedraTokenService, ApplicationProperties applicationProperties) {
        this.catedraTokenService = catedraTokenService;
        this.applicationProperties = applicationProperties;
    }

    @GetMapping
    public ResponseEntity<CatedraTokenStatusDTO> getStatus() {
        boolean present = catedraTokenService.getToken().isPresent();
        String baseUrl = applicationProperties.getCatedra().getBaseUrl();
        return ResponseEntity.ok(new CatedraTokenStatusDTO(present, baseUrl));
    }

    @PutMapping
    public ResponseEntity<Void> updateToken(@Valid @RequestBody CatedraTokenUpdateDTO dto) {
        LOG.info("Actualizando token de cátedra por solicitud administrativa");
        catedraTokenService.updateToken(dto.getToken());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearToken() {
        LOG.warn("Eliminando token de la cátedra por solicitud administrativa");
        catedraTokenService.clearToken();
        return ResponseEntity.noContent().build();
    }
}

