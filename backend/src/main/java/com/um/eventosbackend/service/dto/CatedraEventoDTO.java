package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para mapear eventos recibidos del servicio de la cátedra.
 * <p>
 * Estructura esperada del endpoint GET /api/endpoints/v1/eventos
 */
public class CatedraEventoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("titulo")
    private String titulo;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("resumen")
    private String resumen;

    @JsonProperty("fecha")
    private Instant fecha;

    @JsonProperty("direccion")
    private String direccion;

    @JsonProperty("imagenUrl")
    private String imagenUrl;

    @JsonProperty("precio")
    private BigDecimal precio;

    @JsonProperty("cancelado")
    private Boolean cancelado;

    @JsonProperty("tipo")
    private CatedraEventoTipoDTO tipo;

    @JsonProperty("integrantes")
    private List<CatedraIntegranteDTO> integrantes = new ArrayList<>();

    public CatedraEventoDTO() {
        // Constructor vacío para Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Boolean getCancelado() {
        return cancelado;
    }

    public void setCancelado(Boolean cancelado) {
        this.cancelado = cancelado;
    }

    public CatedraEventoTipoDTO getTipo() {
        return tipo;
    }

    public void setTipo(CatedraEventoTipoDTO tipo) {
        this.tipo = tipo;
    }

    public List<CatedraIntegranteDTO> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<CatedraIntegranteDTO> integrantes) {
        this.integrantes = integrantes != null ? integrantes : new ArrayList<>();
    }
}

