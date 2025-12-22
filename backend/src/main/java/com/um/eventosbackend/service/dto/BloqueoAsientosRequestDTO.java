package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BloqueoAsientosRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("asientos")
    private List<AsientoBloqueoDTO> asientos = new ArrayList<>();

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<AsientoBloqueoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoBloqueoDTO> asientos) {
        this.asientos = asientos;
    }

    public static class AsientoBloqueoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private Integer fila;

        @JsonProperty("columna")
        private Integer columna;

        public Integer getFila() {
            return fila;
        }

        public void setFila(Integer fila) {
            this.fila = fila;
        }

        public Integer getColumna() {
            return columna;
        }

        public void setColumna(Integer columna) {
            this.columna = columna;
        }
    }
}

