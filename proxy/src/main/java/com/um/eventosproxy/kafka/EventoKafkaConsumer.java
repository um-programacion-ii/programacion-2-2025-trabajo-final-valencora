package com.um.eventosproxy.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.dto.EventoChangeDTO;
import com.um.eventosproxy.dto.NotificacionEventoDTO;
import com.um.eventosproxy.service.BackendNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventoKafkaConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EventoKafkaConsumer.class);

    private final BackendNotificationService backendNotificationService;
    private final ObjectMapper objectMapper;

    public EventoKafkaConsumer(
        BackendNotificationService backendNotificationService,
        ObjectMapper objectMapper
    ) {
        this.backendNotificationService = backendNotificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${application.kafka.topic.eventos}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEventoChange(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        LOG.info("Mensaje recibido de Kafka - Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);
        LOG.debug("Contenido del mensaje: {}", message);

        try {
            // Deserializar mensaje
            EventoChangeDTO eventoChange = objectMapper.readValue(message, EventoChangeDTO.class);

            if (eventoChange == null || eventoChange.getEventoId() == null) {
                LOG.warn("Mensaje de Kafka inválido o sin eventoId, se omite");
                acknowledgment.acknowledge();
                return;
            }

            LOG.info("Procesando cambio de evento: eventoId={}, tipoCambio={}", 
                eventoChange.getEventoId(), eventoChange.getTipoCambio());

            // Convertir a DTO de notificación
            NotificacionEventoDTO notificacion = convertirANotificacion(eventoChange);

            // Notificar al backend de forma asíncrona
            backendNotificationService.notificarCambioEvento(notificacion)
                .thenRun(() -> {
                    LOG.debug("Notificación procesada exitosamente para eventoId: {}", eventoChange.getEventoId());
                    acknowledgment.acknowledge();
                })
                .exceptionally(ex -> {
                    LOG.error("Error al procesar notificación para eventoId: {}", eventoChange.getEventoId(), ex);
                    // Aún así confirmamos el mensaje para evitar reprocesamiento infinito
                    acknowledgment.acknowledge();
                    return null;
                });

        } catch (Exception e) {
            LOG.error("Error al procesar mensaje de Kafka", e);
            // Confirmar mensaje para evitar bloqueo del consumer
            acknowledgment.acknowledge();
        }
    }

    private NotificacionEventoDTO convertirANotificacion(EventoChangeDTO eventoChange) {
        NotificacionEventoDTO notificacion = new NotificacionEventoDTO();
        notificacion.setEventoIdCatedra(eventoChange.getEventoId());

        // Mapear tipo de cambio
        switch (eventoChange.getTipoCambio()) {
            case CREATE:
                notificacion.setTipoCambio(NotificacionEventoDTO.TipoCambio.CREATE);
                break;
            case UPDATE:
                notificacion.setTipoCambio(NotificacionEventoDTO.TipoCambio.UPDATE);
                break;
            case DELETE:
                notificacion.setTipoCambio(NotificacionEventoDTO.TipoCambio.DELETE);
                break;
            case CANCEL:
                notificacion.setTipoCambio(NotificacionEventoDTO.TipoCambio.CANCEL);
                break;
            default:
                LOG.warn("Tipo de cambio desconocido: {}, usando UPDATE por defecto", eventoChange.getTipoCambio());
                notificacion.setTipoCambio(NotificacionEventoDTO.TipoCambio.UPDATE);
        }

        // Incluir datos del evento si están disponibles
        notificacion.setEvento(eventoChange.getEvento());

        return notificacion;
    }
}

