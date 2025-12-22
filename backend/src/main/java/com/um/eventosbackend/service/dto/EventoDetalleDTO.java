package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de detalle para un evento.
 */
public class EventoDetalleDTO implements Serializable {

    private Long id;
    private Long eventoIdCatedra;
    private String titulo;
    private String descripcion;
    private String resumen;
    private Instant fecha;
    private String direccion;
    private String imagenUrl;
    private BigDecimal precio;
    private Boolean cancelado;

    private String tipoNombre;
    private String tipoDescripcion;

    private Integer filaAsientos;
    private Integer columnAsientos;

    private List<IntegranteDTO> integrantes = new ArrayList<>();

    @JsonProperty("asientos")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<AsientoBloqueadoDTO> asientos;

    public static class AsientoBloqueadoDTO implements Serializable {
        private Integer fila;
        private Integer columna;
        private String estado;

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

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    public static class IntegranteDTO implements Serializable {
        private String nombre;
        private String descripcion;
        private String imagenUrl;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getImagenUrl() {
            return imagenUrl;
        }

        public void setImagenUrl(String imagenUrl) {
            this.imagenUrl = imagenUrl;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventoIdCatedra() {
        return eventoIdCatedra;
    }

    public void setEventoIdCatedra(Long eventoIdCatedra) {
        this.eventoIdCatedra = eventoIdCatedra;
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

    public String getTipoNombre() {
        return tipoNombre;
    }

    public void setTipoNombre(String tipoNombre) {
        this.tipoNombre = tipoNombre;
    }

    public String getTipoDescripcion() {
        return tipoDescripcion;
    }

    public void setTipoDescripcion(String tipoDescripcion) {
        this.tipoDescripcion = tipoDescripcion;
    }

    public List<IntegranteDTO> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<IntegranteDTO> integrantes) {
        this.integrantes = integrantes != null ? integrantes : new ArrayList<>();
    }

    public List<AsientoBloqueadoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoBloqueadoDTO> asientos) {
        this.asientos = asientos;
    }

    public Integer getFilaAsientos() {
        return filaAsientos;
    }

    public void setFilaAsientos(Integer filaAsientos) {
        this.filaAsientos = filaAsientos;
    }

    public Integer getColumnAsientos() {
        return columnAsientos;
    }

    public void setColumnAsientos(Integer columnAsientos) {
        this.columnAsientos = columnAsientos;
    }
}