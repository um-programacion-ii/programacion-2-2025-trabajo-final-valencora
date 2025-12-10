package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class NotificacionEventoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoIdCatedra")
    private Long eventoIdCatedra;

    @JsonProperty("tipoCambio")
    private TipoCambio tipoCambio;

    @JsonProperty("evento")
    private Object evento; // DTO completo del evento (puede ser null si es DELETE)

    public Long getEventoIdCatedra() {
        return eventoIdCatedra;
    }

    public void setEventoIdCatedra(Long eventoIdCatedra) {
        this.eventoIdCatedra = eventoIdCatedra;
    }

    public TipoCambio getTipoCambio() {
        return tipoCambio;
    }

    public void setTipoCambio(TipoCambio tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public Object getEvento() {
        return evento;
    }

    public void setEvento(Object evento) {
        this.evento = evento;
    }

    public enum TipoCambio {
        CREATE,
        UPDATE,
        DELETE,
        CANCEL
    }
}

