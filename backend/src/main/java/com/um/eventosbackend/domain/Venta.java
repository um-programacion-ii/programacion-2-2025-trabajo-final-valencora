package com.um.eventosbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad Venta que representa una venta de asientos para un evento.
 */
@Entity
@Table(name = "venta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Venta extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "venta_seq")
    @SequenceGenerator(name = "venta_seq", sequenceName = "venta_seq", allocationSize = 1)
    private Long id;

    /**
     * ID de la venta en el servicio de la cátedra (si fue confirmada)
     */
    @Column(name = "venta_id_catedra")
    private Long ventaIdCatedra;

    /**
     * ID del evento para el cual se realizó la venta
     */
    @NotNull
    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    /**
     * Usuario que realizó la venta
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties(value = { "authorities", "activated" }, allowSetters = true)
    private User usuario;

    /**
     * Fecha de la venta
     */
    @NotNull
    @Column(name = "fecha_venta", nullable = false)
    private Instant fechaVenta;

    /**
     * Precio total de la venta
     */
    @Column(name = "precio_venta", precision = 21, scale = 2)
    private BigDecimal precioVenta;

    /**
     * Resultado de la venta: EXITOSA, FALLIDA, PENDIENTE
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoVenta resultado;

    /**
     * Mensaje de error o información adicional
     */
    @Column(name = "mensaje", length = 500)
    private String mensaje;

    /**
     * Número de intentos de confirmación con la cátedra
     */
    @Column(name = "intentos_reintento", nullable = false)
    private Integer intentosReintento = 0;

    /**
     * Fecha del último intento de reintento
     */
    @Column(name = "ultimo_intento_reintento")
    private Instant ultimoIntentoReintento;

    /**
     * Asientos asociados a esta venta
     */
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "venta" }, allowSetters = true)
    private Set<AsientoVenta> asientos = new HashSet<>();

    public enum ResultadoVenta {
        EXITOSA,
        FALLIDA,
        PENDIENTE
    }

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

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
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

    public ResultadoVenta getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoVenta resultado) {
        this.resultado = resultado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Integer getIntentosReintento() {
        return intentosReintento;
    }

    public void setIntentosReintento(Integer intentosReintento) {
        this.intentosReintento = intentosReintento;
    }

    public Instant getUltimoIntentoReintento() {
        return ultimoIntentoReintento;
    }

    public void setUltimoIntentoReintento(Instant ultimoIntentoReintento) {
        this.ultimoIntentoReintento = ultimoIntentoReintento;
    }

    public Set<AsientoVenta> getAsientos() {
        return asientos;
    }

    public void setAsientos(Set<AsientoVenta> asientos) {
        this.asientos = asientos;
    }

    public Venta addAsiento(AsientoVenta asiento) {
        this.asientos.add(asiento);
        asiento.setVenta(this);
        return this;
    }

    public Venta removeAsiento(AsientoVenta asiento) {
        this.asientos.remove(asiento);
        asiento.setVenta(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venta)) {
            return false;
        }
        return id != null && id.equals(((Venta) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Venta{" +
            "id=" + id +
            ", ventaIdCatedra=" + ventaIdCatedra +
            ", eventoId=" + eventoId +
            ", fechaVenta=" + fechaVenta +
            ", precioVenta=" + precioVenta +
            ", resultado=" + resultado +
            '}';
    }
}

