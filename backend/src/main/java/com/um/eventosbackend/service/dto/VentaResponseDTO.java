package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VentaResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("ventaIdCatedra")
    private Long ventaIdCatedra;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("fechaVenta")
    private Instant fechaVenta;

    @JsonProperty("precioVenta")
    private BigDecimal precioVenta;

    @JsonProperty("resultado")
    private String resultado;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("asientos")
    private List<AsientoVentaDTO> asientos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVentaIdCatedra() {
        return ventaIdCatedra;
    }

    public void setVentaIdCatedra(Long ventaIdCatedra) {
        this.ventaIdCatedra = ventaIdCatedra;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public Instant getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(Instant fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
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

