package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VentaRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("asientos")
    private List<AsientoVentaDTO> asientos = new ArrayList<>();

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<AsientoVentaDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoVentaDTO> asientos) {
        this.asientos = asientos;
    }

    public static class AsientoVentaDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private String fila;

        @JsonProperty("numero")
        private Integer numero;

        @JsonProperty("nombrePersona")
        private String nombrePersona;

        @JsonProperty("apellidoPersona")
        private String apellidoPersona;

        public String getFila() {
            return fila;
        }

        public void setFila(String fila) {
            this.fila = fila;
        }

        public Integer getNumero() {
            return numero;
        }

        public void setNumero(Integer numero) {
            this.numero = numero;
        }

        public String getNombrePersona() {
            return nombrePersona;
        }

        public void setNombrePersona(String nombrePersona) {
            this.nombrePersona = nombrePersona;
        }

        public String getApellidoPersona() {
            return apellidoPersona;
        }

        public void setApellidoPersona(String apellidoPersona) {
            this.apellidoPersona = apellidoPersona;
        }
    }
}

