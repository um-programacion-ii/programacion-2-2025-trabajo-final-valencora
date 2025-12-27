package com.um.eventosproxy.kafka;

import com.um.eventosproxy.service.BackendSyncService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
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
        errorHandler = "kafkaErrorHandler",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEventoChange(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        int partition = record.partition();
        long offset = record.offset();
        String key = record.key();
        String value = record.value();

        LOG.info("📩 Mensaje Kafka recibido. topic={}, partition={}, offset={}, key={}, value={}",
            topic, partition, offset, key, value);

        try {
            // Sincronizar eventos con el backend
            // Cuando se recibe un mensaje de Kafka, se hace un "sync completo" de eventos en el backend
            backendSyncService.syncEventsWithBackend();
            LOG.debug("✅ Mensaje Kafka procesado y sincronización iniciada exitosamente");
        } catch (Exception e) {
            LOG.error("❌ Error al procesar mensaje de Kafka", e);
        }
    }
}