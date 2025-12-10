package com.um.eventosbackend.service.catedra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.service.dto.CatedraEventoDTO;
import com.um.eventosbackend.service.dto.NotificacionEventoDTO;
import com.um.eventosbackend.service.mapper.EventoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventoNotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(EventoNotificacionService.class);

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;
    private final ObjectMapper objectMapper;

    public EventoNotificacionService(
        EventoRepository eventoRepository,
        EventoMapper eventoMapper,
        ObjectMapper objectMapper
    ) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
        this.objectMapper = objectMapper;
    }

    public void procesarNotificacion(NotificacionEventoDTO notificacion) {
        if (notificacion == null || notificacion.getEventoIdCatedra() == null) {
            LOG.warn("Notificación inválida o sin eventoIdCatedra, se omite");
            return;
        }

        LOG.info("Procesando notificación de cambio de evento: eventoIdCatedra={}, tipoCambio={}", 
            notificacion.getEventoIdCatedra(), notificacion.getTipoCambio());

        try {
            switch (notificacion.getTipoCambio()) {
                case CREATE:
                    procesarCreacion(notificacion);
                    break;
                case UPDATE:
                    procesarActualizacion(notificacion);
                    break;
                case DELETE:
                    procesarEliminacion(notificacion);
                    break;
                case CANCEL:
                    procesarCancelacion(notificacion);
                    break;
                default:
                    LOG.warn("Tipo de cambio desconocido: {}, se omite", notificacion.getTipoCambio());
            }
        } catch (Exception e) {
            LOG.error("Error al procesar notificación de evento", e);
            throw new RuntimeException("Error al procesar notificación de evento", e);
        }
    }

    private void procesarCreacion(NotificacionEventoDTO notificacion) {
        LOG.debug("Procesando creación de evento: eventoIdCatedra={}", notificacion.getEventoIdCatedra());

        // Verificar si ya existe
        eventoRepository.findByEventoIdCatedra(notificacion.getEventoIdCatedra())
            .ifPresent(eventoExistente -> {
                LOG.warn("Evento con eventoIdCatedra={} ya existe, se actualiza en lugar de crear", 
                    notificacion.getEventoIdCatedra());
                procesarActualizacion(notificacion);
                return;
            });

        // Convertir DTO a entidad
        CatedraEventoDTO eventoDTO = convertirAEventoDTO(notificacion.getEvento());
        if (eventoDTO == null) {
            LOG.warn("No se pudo convertir el evento a DTO, se omite la creación");
            return;
        }

        Evento nuevoEvento = eventoMapper.toEntity(eventoDTO);
        eventoRepository.save(nuevoEvento);
        LOG.info("Evento creado exitosamente: eventoIdCatedra={}", notificacion.getEventoIdCatedra());
    }

    private void procesarActualizacion(NotificacionEventoDTO notificacion) {
        LOG.debug("Procesando actualización de evento: eventoIdCatedra={}", notificacion.getEventoIdCatedra());

        Evento eventoExistente = eventoRepository.findByEventoIdCatedra(notificacion.getEventoIdCatedra())
            .orElse(null);

        if (eventoExistente == null) {
            LOG.warn("Evento con eventoIdCatedra={} no existe, se crea en lugar de actualizar", 
                notificacion.getEventoIdCatedra());
            procesarCreacion(notificacion);
            return;
        }

        // Convertir DTO a entidad
        CatedraEventoDTO eventoDTO = convertirAEventoDTO(notificacion.getEvento());
        if (eventoDTO == null) {
            LOG.warn("No se pudo convertir el evento a DTO, se omite la actualización");
            return;
        }

        eventoMapper.updateEntityFromDTO(eventoExistente, eventoDTO);
        eventoRepository.save(eventoExistente);
        LOG.info("Evento actualizado exitosamente: eventoIdCatedra={}", notificacion.getEventoIdCatedra());
    }

    private void procesarEliminacion(NotificacionEventoDTO notificacion) {
        LOG.debug("Procesando eliminación de evento: eventoIdCatedra={}", notificacion.getEventoIdCatedra());

        eventoRepository.findByEventoIdCatedra(notificacion.getEventoIdCatedra())
            .ifPresentOrElse(
                evento -> {
                    // No eliminar eventos cancelados
                    if (evento.getCancelado() != null && evento.getCancelado()) {
                        LOG.info("Evento con eventoIdCatedra={} está cancelado, no se elimina", 
                            notificacion.getEventoIdCatedra());
                        return;
                    }
                    eventoRepository.delete(evento);
                    LOG.info("Evento eliminado exitosamente: eventoIdCatedra={}", notificacion.getEventoIdCatedra());
                },
                () -> LOG.warn("Evento con eventoIdCatedra={} no existe, no se puede eliminar", 
                    notificacion.getEventoIdCatedra())
            );
    }

    private void procesarCancelacion(NotificacionEventoDTO notificacion) {
        LOG.debug("Procesando cancelación de evento: eventoIdCatedra={}", notificacion.getEventoIdCatedra());

        eventoRepository.findByEventoIdCatedra(notificacion.getEventoIdCatedra())
            .ifPresentOrElse(
                evento -> {
                    evento.setCancelado(true);
                    eventoRepository.save(evento);
                    LOG.info("Evento cancelado exitosamente: eventoIdCatedra={}", notificacion.getEventoIdCatedra());
                },
                () -> {
                    LOG.warn("Evento con eventoIdCatedra={} no existe, se intenta crear como cancelado", 
                        notificacion.getEventoIdCatedra());
                    // Si el evento no existe, intentar crearlo como cancelado
                    CatedraEventoDTO eventoDTO = convertirAEventoDTO(notificacion.getEvento());
                    if (eventoDTO != null) {
                        eventoDTO.setCancelado(true);
                        Evento nuevoEvento = eventoMapper.toEntity(eventoDTO);
                        eventoRepository.save(nuevoEvento);
                        LOG.info("Evento creado como cancelado: eventoIdCatedra={}", notificacion.getEventoIdCatedra());
                    }
                }
            );
    }

    private CatedraEventoDTO convertirAEventoDTO(Object eventoObj) {
        if (eventoObj == null) {
            return null;
        }

        try {
            if (eventoObj instanceof CatedraEventoDTO) {
                return (CatedraEventoDTO) eventoObj;
            }

            // Intentar convertir desde Map o JSON
            return objectMapper.convertValue(eventoObj, CatedraEventoDTO.class);
        } catch (Exception e) {
            LOG.error("Error al convertir objeto a CatedraEventoDTO", e);
            return null;
        }
    }
}

