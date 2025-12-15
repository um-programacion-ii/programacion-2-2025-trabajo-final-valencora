package com.um.eventosproxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.um.eventosproxy.service.catedra.CatedraTokenInterceptor;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ProxyProperties.class)
@EnableAsync
public class ProxyConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Bean(name = "catedraRestTemplate")
    public RestTemplate catedraRestTemplate(
        RestTemplateBuilder builder,
        ProxyProperties proxyProperties,
        CatedraTokenInterceptor catedraTokenInterceptor
    ) {
        ProxyProperties.Catedra catedraProps = proxyProperties.getCatedra();

        if (catedraProps.getBaseUrl() == null || catedraProps.getBaseUrl().isBlank()) {
            throw new IllegalStateException("Debe configurar application.catedra.base-url para invocar a la cátedra");
        }

        return builder
            .rootUri(catedraProps.getBaseUrl())
            .setConnectTimeout(Duration.ofMillis(5000))
            .setReadTimeout(Duration.ofMillis(10000))
            .interceptors(List.of(catedraTokenInterceptor))
            .build();
    }

    @Bean
    public CatedraTokenInterceptor catedraTokenInterceptor(ProxyProperties proxyProperties) {
        return new CatedraTokenInterceptor(proxyProperties);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

