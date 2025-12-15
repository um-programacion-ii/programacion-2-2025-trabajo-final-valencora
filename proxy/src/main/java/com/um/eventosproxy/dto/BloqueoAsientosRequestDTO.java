package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class BloqueoAsientosRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("asientos")
    private List<AsientoBloqueoDTO> asientos;

    @Data
    public static class AsientoBloqueoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private Integer fila;

        @JsonProperty("columna")
        private Integer columna;
    }
}

