package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.security.AuthoritiesConstants;
import com.um.eventosbackend.service.catedra.EventoSincronizacionService;
import com.um.eventosbackend.service.catedra.exception.CatedraAuthenticationException;
import com.um.eventosbackend.service.catedra.exception.MissingCatedraTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * REST controller para sincronizar eventos desde el servicio de la cátedra.
 */
@RestController
@RequestMapping("/api/admin/eventos")
@PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")
public class EventoSincronizacionResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventoSincronizacionResource.class);

    private final EventoSincronizacionService eventoSincronizacionService;

    public EventoSincronizacionResource(EventoSincronizacionService eventoSincronizacionService) {
        this.eventoSincronizacionService = eventoSincronizacionService;
    }

    /**
     * {@code POST /api/admin/eventos/sincronizar} : Sincroniza eventos desde el servicio de la cátedra.
     * <p>
     * Este endpoint obtiene todos los eventos del servicio de la cátedra y los sincroniza
     * con la base de datos local. Requiere rol ADMIN y token de cátedra configurado.
     *
     * @return {@link ResponseEntity} con status {@code 200 (OK)} si la sincronización fue exitosa,
     *         o un error específico según el problema encontrado.
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<String> sincronizarEventos() {
        LOG.info("REST request para sincronizar eventos desde el servicio de la cátedra");
        try {
            eventoSincronizacionService.sincronizarEventos();
            LOG.info("Sincronización de eventos completada exitosamente");
            return ResponseEntity.ok().build();
        } catch (MissingCatedraTokenException e) {
            LOG.error("No se puede sincronizar eventos: token de cátedra no configurado", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("No hay token configurado para invocar al servicio de la cátedra. Configure el token primero.");
        } catch (CatedraAuthenticationException e) {
            LOG.error("Error de autenticación con el servicio de la cátedra", e);
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Token inválido o expirado para el servicio de la cátedra. Actualice el token.");
        } catch (HttpClientErrorException e) {
            LOG.error("Error HTTP del cliente al sincronizar eventos: {} - {}", e.getStatusCode(), e.getMessage(), e);
            return ResponseEntity
                .status(e.getStatusCode())
                .body("Error al comunicarse con el servicio de la cátedra: " + e.getMessage());
        } catch (ResourceAccessException e) {
            LOG.error("Error de conexión con el servicio de la cátedra", e);
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("No se pudo conectar con el servicio de la cátedra. Verifique la conexión a ZeroTier.");
        } catch (RuntimeException e) {
            LOG.error("Error al sincronizar eventos: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Error al sincronizar eventos: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("Error inesperado al sincronizar eventos", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error inesperado al sincronizar eventos: " + e.getMessage());
        }
    }
}