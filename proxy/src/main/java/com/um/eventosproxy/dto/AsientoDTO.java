package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

@Data
public class AsientoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("fila")
    private String fila;

    @JsonProperty("numero")
    private Integer numero;

    @JsonProperty("estado")
    private EstadoAsiento estado;

    public enum EstadoAsiento {
        LIBRE,
        OCUPADO,
        BLOQUEADO
    }
}

