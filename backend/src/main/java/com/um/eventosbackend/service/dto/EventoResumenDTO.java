package com.um.eventosbackend.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resumen para listar eventos.
 */
public class EventoResumenDTO implements Serializable {

    private Long id;
    private String titulo;
    private String resumen;
    private Instant fecha;
    private String direccion;
    private BigDecimal precio;
    private Boolean cancelado;

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
}


