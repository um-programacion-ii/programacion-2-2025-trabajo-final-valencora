package com.um.eventosproxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;

/**
 * Configuración de Kafka con manejo de errores mejorado.
 * Permite que la aplicación arranque incluso si Kafka no está disponible.
 */
@Configuration
@EnableKafka
public class KafkaConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

    /**
     * Bean para manejar errores en los listeners de Kafka.
     * Esto permite que la aplicación continúe funcionando aunque haya errores
     * al procesar mensajes de Kafka.
     */
    @org.springframework.context.annotation.Bean
    public ConsumerAwareListenerErrorHandler kafkaErrorHandler() {
        return (message, exception, consumer) -> {
            LOG.error("❌ Error al procesar mensaje de Kafka: {}", exception.getMessage(), exception);
            // No re-lanzar la excepción para evitar que el consumer se detenga
            return null;
        };
    }
}

