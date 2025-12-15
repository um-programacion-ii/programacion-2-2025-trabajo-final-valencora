package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapaAsientosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("asientos")
    private List<AsientoDTO> asientos = new ArrayList<>();

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<AsientoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoDTO> asientos) {
        this.asientos = asientos;
    }

    public static class AsientoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private String fila;

        @JsonProperty("numero")
        private Integer numero;

        @JsonProperty("estado")
        private EstadoAsiento estado;

        @JsonProperty("seleccionado")
        private Boolean seleccionado = false;

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

        public EstadoAsiento getEstado() {
            return estado;
        }

        public void setEstado(EstadoAsiento estado) {
            this.estado = estado;
        }

        public Boolean getSeleccionado() {
            return seleccionado;
        }

        public void setSeleccionado(Boolean seleccionado) {
            this.seleccionado = seleccionado;
        }

        public enum EstadoAsiento {
            LIBRE,
            OCUPADO,
            BLOQUEADO;

            @JsonCreator
            public static EstadoAsiento fromString(String value) {
                if (value == null) {
                    return null;
                }
                try {
                    return EstadoAsiento.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Si no coincide exactamente, intentar con diferentes variaciones
                    String upperValue = value.toUpperCase().trim();
                    for (EstadoAsiento estado : EstadoAsiento.values()) {
                        if (estado.name().equals(upperValue)) {
                            return estado;
                        }
                    }
                    return null;
                }
            }

            @JsonValue
            public String toValue() {
                return this.name();
            }
        }
    }
}

