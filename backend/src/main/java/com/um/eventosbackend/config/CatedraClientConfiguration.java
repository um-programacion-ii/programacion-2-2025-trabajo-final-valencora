package com.um.eventosbackend.config;

import com.um.eventosbackend.service.catedra.http.CatedraTokenInterceptor;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CatedraClientConfiguration {

    @Bean(name = "catedraRestTemplate")
    public RestTemplate catedraRestTemplate(
        RestTemplateBuilder builder,
        ApplicationProperties applicationProperties,
        CatedraTokenInterceptor tokenInterceptor
    ) {
        ApplicationProperties.Catedra catedraProps = applicationProperties.getCatedra();

        if (catedraProps.getBaseUrl() == null || catedraProps.getBaseUrl().isBlank()) {
            throw new IllegalStateException("Debe configurar application.catedra.base-url para invocar a la cátedra");
        }

        RestTemplate restTemplate = builder
            .rootUri(catedraProps.getBaseUrl())
            .setConnectTimeout(Duration.ofMillis(catedraProps.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(catedraProps.getReadTimeoutMs()))
            .additionalInterceptors(tokenInterceptor)
            .build();

        // Buffering permite leer el body en interceptores si se necesitara para logging/manejo de errores.
        restTemplate.setRequestFactory(
            new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
        );
        return restTemplate;
    }
}

