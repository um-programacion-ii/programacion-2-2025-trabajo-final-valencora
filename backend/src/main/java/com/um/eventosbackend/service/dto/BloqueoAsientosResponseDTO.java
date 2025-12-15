package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BloqueoAsientosResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("exitoso")
    private Boolean exitoso;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("asientosBloqueados")
    private List<AsientoBloqueoDTO> asientosBloqueados = new ArrayList<>();

    @JsonProperty("asientosNoDisponibles")
    private List<AsientoBloqueoDTO> asientosNoDisponibles = new ArrayList<>();

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<AsientoBloqueoDTO> getAsientosBloqueados() {
        return asientosBloqueados;
    }

    public void setAsientosBloqueados(List<AsientoBloqueoDTO> asientosBloqueados) {
        this.asientosBloqueados = asientosBloqueados;
    }

    public List<AsientoBloqueoDTO> getAsientosNoDisponibles() {
        return asientosNoDisponibles;
    }

    public void setAsientosNoDisponibles(List<AsientoBloqueoDTO> asientosNoDisponibles) {
        this.asientosNoDisponibles = asientosNoDisponibles;
    }

    public static class AsientoBloqueoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private String fila;

        @JsonProperty("numero")
        private Integer numero;

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
    }
}

