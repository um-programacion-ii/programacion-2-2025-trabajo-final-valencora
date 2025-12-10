package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class EventoChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("tipoCambio")
    private TipoCambio tipoCambio;

    @JsonProperty("evento")
    private Object evento; // Puede ser null si es DELETE

    public enum TipoCambio {
        CREATE,
        UPDATE,
        DELETE,
        CANCEL
    }
}

