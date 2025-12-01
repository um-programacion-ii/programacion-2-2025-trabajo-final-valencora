package com.um.eventosbackend.service.dto;

import jakarta.validation.constraints.NotBlank;

public class CatedraTokenUpdateDTO {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

