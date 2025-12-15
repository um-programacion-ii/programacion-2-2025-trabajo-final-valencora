package com.um.eventosbackend.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProxyClientConfiguration {

    @Bean(name = "proxyRestTemplate")
    public RestTemplate proxyRestTemplate(
        RestTemplateBuilder builder,
        ApplicationProperties applicationProperties
    ) {
        ApplicationProperties.Proxy proxyProps = applicationProperties.getProxy();

        if (proxyProps.getBaseUrl() == null || proxyProps.getBaseUrl().isBlank()) {
            throw new IllegalStateException("Debe configurar application.proxy.base-url para invocar al proxy");
        }

        return builder
            .rootUri(proxyProps.getBaseUrl())
            .setConnectTimeout(Duration.ofMillis(5000))
            .setReadTimeout(Duration.ofMillis(10000))
            .build();
    }
}

