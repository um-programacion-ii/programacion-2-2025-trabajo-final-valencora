package com.um.eventosbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Backend.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();
    private final Catedra catedra = new Catedra();
    private final Proxy proxy = new Proxy();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Catedra getCatedra() {
        return catedra;
    }

    public Proxy getProxy() {
        return proxy;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    public static class Catedra {

        private String baseUrl;
        private String registrationEndpoint = "/api/registro";
        /**
         * Token entregado por la cátedra. Se recomienda inyectarlo mediante variable de entorno.
         */
        private String authToken;
        private Integer connectTimeoutMs = 5000;
        private Integer readTimeoutMs = 10000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getRegistrationEndpoint() {
            return registrationEndpoint;
        }

        public void setRegistrationEndpoint(String registrationEndpoint) {
            this.registrationEndpoint = registrationEndpoint;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public Integer getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(Integer readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Proxy {
        private String baseUrl = "http://localhost:8081";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
    // jhipster-needle-application-properties-property-class
}
