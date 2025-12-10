package com.um.eventosproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ProxyProperties {

    private final Backend backend = new Backend();
    private final Kafka kafka = new Kafka();

    public Backend getBackend() {
        return backend;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Backend {
        private String baseUrl = "http://localhost:8080";
        private final Jwt jwt = new Jwt();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            private String secret;
            private Long tokenValidityInSeconds = 86400L;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public Long getTokenValidityInSeconds() {
                return tokenValidityInSeconds;
            }

            public void setTokenValidityInSeconds(Long tokenValidityInSeconds) {
                this.tokenValidityInSeconds = tokenValidityInSeconds;
            }
        }
    }

    public static class Kafka {
        private final Topic topic = new Topic();

        public Topic getTopic() {
            return topic;
        }

        public static class Topic {
            private String eventos = "eventos-changes";

            public String getEventos() {
                return eventos;
            }

            public void setEventos(String eventos) {
                this.eventos = eventos;
            }
        }
    }
}

