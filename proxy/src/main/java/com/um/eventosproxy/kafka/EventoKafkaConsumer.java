package com.um.eventosproxy.kafka;

import com.um.eventosproxy.service.BackendSyncService;
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

    private final BackendSyncService backendSyncService;

    public EventoKafkaConsumer(BackendSyncService backendSyncService) {
        this.backendSyncService = backendSyncService;
        LOG.info("📦 EventoKafkaConsumer inicializado. El listener se conectará cuando Kafka esté disponible.");
    }

    @KafkaListener(
        topics = "${application.kafka.topic.eventos}", 
        groupId = "${spring.kafka.consumer.group-id}",
        errorHandler = "kafkaErrorHandler"
    )
    public void consumeEventoChange(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) {
        LOG.info("📩 Mensaje Kafka recibido. topic={}, partition={}, offset={}, key={}, value={}",
            topic, partition, offset, "N/A", message);

        try {
            // Sincronizar eventos con el backend 
            // Cuando se recibe un mensaje de Kafka, se hace un "sync completo" de eventos en el backend
            backendSyncService.syncEventsWithBackend();

            // Confirmar mensaje procesado
            acknowledgment.acknowledge();
            LOG.debug("✅ Mensaje Kafka procesado y sincronización iniciada exitosamente");

        } catch (Exception e) {
            LOG.error("❌ Error al procesar mensaje de Kafka", e);
            // Confirmar mensaje para evitar bloqueo del consumer
            // En caso de error, aún confirmamos para no bloquear el procesamiento de otros mensajes
            acknowledgment.acknowledge();
        }
    }
}
