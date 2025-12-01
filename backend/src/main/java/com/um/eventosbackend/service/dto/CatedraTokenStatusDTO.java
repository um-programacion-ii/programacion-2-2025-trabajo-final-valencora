package com.um.eventosbackend.service.dto;

public class CatedraTokenStatusDTO {

    private boolean tokenPresent;
    private String baseUrl;

    public CatedraTokenStatusDTO() {}

    public CatedraTokenStatusDTO(boolean tokenPresent, String baseUrl) {
        this.tokenPresent = tokenPresent;
        this.baseUrl = baseUrl;
    }

    public boolean isTokenPresent() {
        return tokenPresent;
    }

    public void setTokenPresent(boolean tokenPresent) {
        this.tokenPresent = tokenPresent;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

