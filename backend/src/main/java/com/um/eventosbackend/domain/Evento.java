package com.um.eventosbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad Evento que representa un evento único (charla, curso, obra de teatro, etc.)
 */
@Entity
@Table(name = "evento")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Evento extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "evento_seq")
    @SequenceGenerator(name = "evento_seq", sequenceName = "evento_seq", allocationSize = 1)
    private Long id;

    /**
     * ID del evento en el servicio de la cátedra (para sincronización)
     */
    @NotNull
    @Column(name = "evento_id_catedra", nullable = false, unique = true)
    private Long eventoIdCatedra;

    @NotNull
    @Size(max = 200)
    @Column(name = "titulo", length = 200, nullable = false)
    private String titulo;

    @Size(max = 2000)
    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Size(max = 500)
    @Column(name = "resumen", length = 500)
    private String resumen;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Size(max = 200)
    @Column(name = "direccion", length = 200)
    private String direccion;

    @Size(max = 500)
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "precio", precision = 21, scale = 2)
    private BigDecimal precio;

    /**
     * Indica si el evento está cancelado
     */
    @NotNull
    @Column(name = "cancelado", nullable = false)
    private Boolean cancelado = false;

    /**
     * Relación con el tipo de evento
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id")
    @JsonIgnoreProperties(value = { "eventos" }, allowSetters = true)
    private EventoTipo tipo;

    /**
     * Integrantes o participantes del evento
     */
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento" }, allowSetters = true)
    private Set<Integrante> integrantes = new HashSet<>();

    /**
     * Número de filas de asientos del evento
     */
    @Column(name = "fila_asientos")
    private Integer filaAsiento;

    /**
     * Número de columnas de asientos del evento
     */
    @Column(name = "column_asientos")
    private Integer columnAsiento;

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

    public EventoTipo getTipo() {
        return tipo;
    }

    public void setTipo(EventoTipo tipo) {
        this.tipo = tipo;
    }

    public Set<Integrante> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(Set<Integrante> integrantes) {
        this.integrantes = integrantes;
    }

    public Evento addIntegrante(Integrante integrante) {
        this.integrantes.add(integrante);
        integrante.setEvento(this);
        return this;
    }

    public Evento removeIntegrante(Integrante integrante) {
        this.integrantes.remove(integrante);
        integrante.setEvento(null);
        return this;
    }

    public Integer getFilaAsiento() {
        return filaAsiento;
    }

    public void setFilaAsiento(Integer filaAsiento) {
        this.filaAsiento = filaAsiento;
    }

    public Integer getColumnAsiento() {
        return columnAsiento;
    }

    public void setColumnAsiento(Integer columnAsiento) {
        this.columnAsiento = columnAsiento;
    }

    /**
     * Verifica si el evento está expirado (fecha pasada)
     */
    public boolean isExpirado() {
        return fecha != null && fecha.isBefore(Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Evento)) {
            return false;
        }
        return id != null && id.equals(((Evento) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Evento{" +
            "id=" + id +
            ", eventoIdCatedra=" + eventoIdCatedra +
            ", titulo='" + titulo + '\'' +
            ", fecha=" + fecha +
            ", cancelado=" + cancelado +
            '}';
    }
}