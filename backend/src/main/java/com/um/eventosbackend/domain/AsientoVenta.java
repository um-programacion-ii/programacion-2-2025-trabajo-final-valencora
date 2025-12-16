package com.um.eventosbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad AsientoVenta que representa la relación entre una venta y un asiento.
 */
@Entity
@Table(name = "asiento_venta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AsientoVenta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asiento_venta_seq")
    @SequenceGenerator(name = "asiento_venta_seq", sequenceName = "asiento_venta_seq", allocationSize = 1)
    private Long id;

    /**
     * Venta a la que pertenece este asiento
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonIgnoreProperties(value = { "asientos" }, allowSetters = true)
    private Venta venta;

    /**
     * Fila del asiento
     */
    @NotNull
    @Column(name = "fila", nullable = false, length = 10)
    private String fila;

    /**
     * Número/columna del asiento
     */
    @NotNull
    @Column(name = "numero", nullable = false)
    private Integer numero;

    /**
     * Nombre de la persona asignada al asiento
     */
    @Column(name = "nombre_persona", length = 100)
    private String nombrePersona;

    /**
     * Apellido de la persona asignada al asiento
     */
    @Column(name = "apellido_persona", length = 100)
    private String apellidoPersona;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AsientoVenta)) {
            return false;
        }
        return id != null && id.equals(((AsientoVenta) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AsientoVenta{" +
            "id=" + id +
            ", fila='" + fila + '\'' +
            ", numero=" + numero +
            ", nombrePersona='" + nombrePersona + '\'' +
            ", apellidoPersona='" + apellidoPersona + '\'' +
            '}';
    }
}

