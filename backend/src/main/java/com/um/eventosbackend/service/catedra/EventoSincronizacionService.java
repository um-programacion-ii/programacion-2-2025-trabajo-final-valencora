package com.um.eventosbackend.service.catedra;

import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.service.catedra.exception.CatedraAuthenticationException;
import com.um.eventosbackend.service.catedra.exception.MissingCatedraTokenException;
import com.um.eventosbackend.service.dto.CatedraEventoDTO;
import com.um.eventosbackend.service.mapper.EventoMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Servicio para sincronizar eventos desde el servicio de la cátedra.
 */
@Service
@Transactional
public class EventoSincronizacionService {

    private static final Logger LOG = LoggerFactory.getLogger(EventoSincronizacionService.class);

    private static final String ENDPOINT_EVENTOS = "/api/endpoints/v1/eventos";

    private final RestTemplate catedraRestTemplate;
    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;

    public EventoSincronizacionService(
        @org.springframework.beans.factory.annotation.Qualifier("catedraRestTemplate") RestTemplate catedraRestTemplate,
        EventoRepository eventoRepository,
        EventoMapper eventoMapper
    ) {
        this.catedraRestTemplate = catedraRestTemplate;
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
    }

    /**
     * Sincroniza todos los eventos desde el servicio de la cátedra.
     * <p>
     * Este método:
     * - Obtiene la lista de eventos del servicio de cátedra
     * - Crea eventos nuevos que no existen localmente
     * - Actualiza eventos existentes
     * - Elimina eventos que ya no están en el servicio de cátedra (excepto los cancelados)
     * - Marca como expirados los eventos cuya fecha ya pasó
     */
    public void sincronizarEventos() {
        LOG.info("Iniciando sincronización de eventos desde el servicio de la cátedra");

        try {
            // Obtener eventos del servicio de cátedra
            List<CatedraEventoDTO> eventosCatedra = obtenerEventosDesdeCatedra();

            if (eventosCatedra == null || eventosCatedra.isEmpty()) {
                LOG.warn("No se recibieron eventos del servicio de la cátedra");
                return;
            }

            LOG.info("Se recibieron {} eventos del servicio de la cátedra", eventosCatedra.size());

            // Obtener IDs de eventos de la cátedra
            Set<Long> idsCatedra = eventosCatedra.stream().map(CatedraEventoDTO::getId).collect(Collectors.toSet());

            // Procesar cada evento recibido
            for (CatedraEventoDTO eventoDTO : eventosCatedra) {
                procesarEvento(eventoDTO);
            }

            // Eliminar eventos que ya no están en el servicio de cátedra (excepto cancelados)
            eliminarEventosNoExistentes(idsCatedra);

            // Marcar eventos expirados
            marcarEventosExpirados();

            LOG.info("Sincronización de eventos completada exitosamente");

        } catch (MissingCatedraTokenException e) {
            LOG.error("No se puede sincronizar eventos: token de cátedra no configurado", e);
            throw e;
        } catch (CatedraAuthenticationException e) {
            LOG.error("Error de autenticación con el servicio de la cátedra", e);
            throw e;
        } catch (HttpClientErrorException e) {
            LOG.error("Error HTTP del cliente al sincronizar eventos: {}", e.getStatusCode(), e);
            throw new RuntimeException("Error al comunicarse con el servicio de la cátedra: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            LOG.error("Error HTTP del servidor al sincronizar eventos: {}", e.getStatusCode(), e);
            throw new RuntimeException("Error en el servicio de la cátedra: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            LOG.error("Error de conexión con el servicio de la cátedra", e);
            throw new RuntimeException("No se pudo conectar con el servicio de la cátedra", e);
        } catch (RestClientException e) {
            LOG.error("Error al consumir el servicio de la cátedra", e);
            throw new RuntimeException("Error al sincronizar eventos: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Error inesperado al sincronizar eventos", e);
            throw new RuntimeException("Error inesperado al sincronizar eventos", e);
        }
    }

    /**
     * Obtiene la lista de eventos desde el servicio de la cátedra.
     */
    private List<CatedraEventoDTO> obtenerEventosDesdeCatedra() {
        LOG.debug("Obteniendo eventos desde el servicio de la cátedra: {}", ENDPOINT_EVENTOS);

        try {
            ResponseEntity<CatedraEventoDTO[]> response = catedraRestTemplate.exchange(
                ENDPOINT_EVENTOS,
                HttpMethod.GET,
                null,
                CatedraEventoDTO[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                LOG.warn("Respuesta vacía o no exitosa del servicio de la cátedra");
                return List.of();
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            LOG.error("Token de autenticación inválido o expirado");
            throw new CatedraAuthenticationException("Token inválido o expirado para el servicio de la cátedra", e);
        }
    }

    /**
     * Procesa un evento individual: lo crea si no existe o lo actualiza si ya existe.
     */
    private void procesarEvento(CatedraEventoDTO eventoDTO) {
        if (eventoDTO == null || eventoDTO.getId() == null) {
            LOG.warn("Evento DTO inválido o sin ID, se omite");
            return;
        }

        eventoRepository
            .findByEventoIdCatedra(eventoDTO.getId())
            .ifPresentOrElse(
                eventoExistente -> {
                    // Actualizar evento existente
                    LOG.debug("Actualizando evento existente con ID cátedra: {}", eventoDTO.getId());
                    eventoMapper.updateEntityFromDTO(eventoExistente, eventoDTO);
                    eventoRepository.save(eventoExistente);
                },
                () -> {
                    // Crear nuevo evento
                    LOG.debug("Creando nuevo evento con ID cátedra: {}", eventoDTO.getId());
                    Evento nuevoEvento = eventoMapper.toEntity(eventoDTO);
                    eventoRepository.save(nuevoEvento);
                }
            );
    }

    /**
     * Elimina eventos locales que ya no existen en el servicio de la cátedra.
     * No elimina eventos cancelados.
     */
    private void eliminarEventosNoExistentes(Set<Long> idsCatedra) {
        LOG.debug("Eliminando eventos que ya no existen en el servicio de la cátedra");

        List<Evento> eventosLocales = eventoRepository.findAll();
        int eliminados = 0;

        for (Evento evento : eventosLocales) {
            // No eliminar eventos cancelados
            if (evento.getCancelado() != null && evento.getCancelado()) {
                continue;
            }

            // Eliminar si no está en la lista de IDs de la cátedra
            if (!idsCatedra.contains(evento.getEventoIdCatedra())) {
                LOG.debug("Eliminando evento local con ID cátedra: {} (ya no existe en servicio de cátedra)", evento.getEventoIdCatedra());
                eventoRepository.delete(evento);
                eliminados++;
            }
        }

        if (eliminados > 0) {
            LOG.info("Se eliminaron {} eventos que ya no existen en el servicio de la cátedra", eliminados);
        }
    }

    /**
     * Marca eventos expirados (fecha pasada) para que no aparezcan en listados.
     * Los eventos expirados se mantienen en la base de datos pero se filtran en las consultas.
     */
    private void marcarEventosExpirados() {
        Instant ahora = Instant.now();
        List<Evento> eventosExpirados = eventoRepository.findEventosExpirados(ahora);

        if (!eventosExpirados.isEmpty()) {
            LOG.info("Se encontraron {} eventos expirados", eventosExpirados.size());
            // Los eventos expirados se filtran en las consultas, no es necesario marcarlos
            // El método isExpirado() en la entidad Evento ya maneja esto
        }
    }
}

