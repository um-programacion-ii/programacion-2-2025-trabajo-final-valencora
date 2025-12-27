package com.um.eventosproxy.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Configuración de Kafka con manejo de errores mejorado.
 * Permite que la aplicación arranque incluso si Kafka no está disponible.
 */
@Configuration
@EnableKafka
public class KafkaConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfiguration.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        
        props.put("client.dns.lookup", "use_all_dns_ips");
        
      
        props.put("metadata.max.age.ms", "300000");
        props.put("connections.max.idle.ms", "540000");
        
        // Configuración para reintentos y timeouts más largos
        props.put("reconnect.backoff.ms", "50");
        props.put("retry.backoff.ms", "100");
        props.put("request.timeout.ms", "30000");
        props.put("session.timeout.ms", "30000");
        props.put("heartbeat.interval.ms", "10000");
        
        LOG.info("🔧 Configurando Kafka Consumer. bootstrap-servers={}, group-id={}", bootstrapServers, groupId);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Agregar listener para detectar cuando el consumer se conecta
        factory.getContainerProperties().setConsumerRebalanceListener(
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    LOG.info("🔄 Particiones revocadas: {}", partitions);
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    LOG.info("✅ Particiones asignadas al consumer: {}", partitions);
                }
            }
        );
        
        LOG.info("🔧 KafkaListenerContainerFactory configurado");
        return factory;
    }

    /**
     * Bean para manejar errores en los listeners de Kafka.
     * Esto permite que la aplicación continúe funcionando aunque haya errores
     * al procesar mensajes de Kafka.
     */
    @Bean
    public ConsumerAwareListenerErrorHandler kafkaErrorHandler() {
        return (message, exception, consumer) -> {
            LOG.error("❌ Error al procesar mensaje de Kafka: {}", exception.getMessage(), exception);
            // No re-lanzar la excepción para evitar que el consumer se detenga
            return null;
        };
    }
}