package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class NotificacionEventoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoIdCatedra")
    private Long eventoIdCatedra;

    @JsonProperty("tipoCambio")
    private TipoCambio tipoCambio;

    @JsonProperty("evento")
    private Object evento; // DTO completo del evento (puede ser null si es DELETE)

    public enum TipoCambio {
        CREATE,
        UPDATE,
        DELETE,
        CANCEL
    }
}

