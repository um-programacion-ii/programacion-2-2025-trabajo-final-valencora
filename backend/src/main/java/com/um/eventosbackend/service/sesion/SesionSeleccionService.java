package com.um.eventosbackend.service.sesion;

import com.um.eventosbackend.service.dto.EstadoSeleccionDTO;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class SesionSeleccionService {

    private static final Logger LOG = LoggerFactory.getLogger(SesionSeleccionService.class);
    private static final long EXPIRACION_MINUTOS = 30;
    private static final long EXPIRACION_MILLIS = EXPIRACION_MINUTOS * 60 * 1000;
    private static final int MAX_ASIENTOS = 4;

    // Almacenamiento en memoria: userId -> EstadoSeleccionDTO
    private final Map<String, EstadoSeleccionDTO> sesiones = new ConcurrentHashMap<>();
    // Timestamps de última actualización por usuario
    private final Map<String, Instant> ultimaActualizacion = new ConcurrentHashMap<>();

    /**
     * Obtiene el estado de selección del usuario.
     */
    public EstadoSeleccionDTO obtenerEstado(String userId) {
        limpiarSesionExpirada(userId);
        EstadoSeleccionDTO estado = sesiones.get(userId);
        if (estado != null) {
            estado.setUltimaActualizacion(ultimaActualizacion.get(userId));
        }
        return estado;
    }

    /**
     * Guarda o actualiza el estado de selección del usuario.
     */
    public void guardarEstado(String userId, EstadoSeleccionDTO estado) {
        if (estado == null) {
            LOG.warn("Intento de guardar estado nulo para usuario: {}", userId);
            return;
        }

        // Validar máximo de asientos
        if (estado.getAsientosSeleccionados() != null && estado.getAsientosSeleccionados().size() > MAX_ASIENTOS) {
            throw new IllegalArgumentException("No se pueden seleccionar más de " + MAX_ASIENTOS + " asientos");
        }

        Instant ahora = Instant.now();
        estado.setUltimaActualizacion(ahora);
        sesiones.put(userId, estado);
        ultimaActualizacion.put(userId, ahora);

        LOG.debug("Estado de selección guardado para usuario: {}, eventoId: {}, asientos: {}", 
            userId, estado.getEventoId(), 
            estado.getAsientosSeleccionados() != null ? estado.getAsientosSeleccionados().size() : 0);
    }

    /**
     * Actualiza el evento seleccionado.
     */
    public void actualizarEventoSeleccionado(String userId, Long eventoId) {
        EstadoSeleccionDTO estado = obtenerEstado(userId);
        if (estado == null) {
            estado = new EstadoSeleccionDTO();
        }
        estado.setEventoId(eventoId);
        guardarEstado(userId, estado);
    }

    /**
     * Actualiza los asientos seleccionados.
     */
    public void actualizarAsientosSeleccionados(String userId, EstadoSeleccionDTO.AsientoSeleccionadoDTO... asientos) {
        if (asientos.length > MAX_ASIENTOS) {
            throw new IllegalArgumentException("No se pueden seleccionar más de " + MAX_ASIENTOS + " asientos");
        }

        EstadoSeleccionDTO estado = obtenerEstado(userId);
        if (estado == null) {
            estado = new EstadoSeleccionDTO();
        }

        estado.getAsientosSeleccionados().clear();
        for (EstadoSeleccionDTO.AsientoSeleccionadoDTO asiento : asientos) {
            estado.getAsientosSeleccionados().add(asiento);
        }

        guardarEstado(userId, estado);
    }

    /**
     * Actualiza los nombres de personas para los asientos seleccionados.
     */
    public void actualizarNombresPersonas(String userId, Map<String, EstadoSeleccionDTO.AsientoSeleccionadoDTO> nombresPorAsiento) {
        EstadoSeleccionDTO estado = obtenerEstado(userId);
        if (estado == null) {
            throw new IllegalStateException("No hay estado de selección para el usuario: " + userId);
        }

        for (EstadoSeleccionDTO.AsientoSeleccionadoDTO asiento : estado.getAsientosSeleccionados()) {
            String clave = asiento.getFila() + "-" + asiento.getNumero();
            EstadoSeleccionDTO.AsientoSeleccionadoDTO actualizado = nombresPorAsiento.get(clave);
            if (actualizado != null) {
                asiento.setNombrePersona(actualizado.getNombrePersona());
                asiento.setApellidoPersona(actualizado.getApellidoPersona());
            }
        }

        guardarEstado(userId, estado);
    }

    /**
     * Limpia el estado de selección del usuario.
     */
    public void limpiarEstado(String userId) {
        sesiones.remove(userId);
        ultimaActualizacion.remove(userId);
        LOG.debug("Estado de selección limpiado para usuario: {}", userId);
    }

    /**
     * Verifica si una sesión está expirada y la limpia si es necesario.
     */
    private void limpiarSesionExpirada(String userId) {
        Instant ultimaAct = ultimaActualizacion.get(userId);
        if (ultimaAct != null) {
            long tiempoInactivo = Instant.now().toEpochMilli() - ultimaAct.toEpochMilli();
            if (tiempoInactivo > EXPIRACION_MILLIS) {
                LOG.debug("Sesión expirada para usuario: {} (inactivo por {} minutos)", userId, tiempoInactivo / 60000);
                limpiarEstado(userId);
            }
        }
    }

    /**
     * Limpia todas las sesiones expiradas (ejecutado periódicamente).
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void limpiarSesionesExpiradas() {
        Instant ahora = Instant.now();
        int limpiadas = 0;

        for (Map.Entry<String, Instant> entry : ultimaActualizacion.entrySet()) {
            String userId = entry.getKey();
            Instant ultimaAct = entry.getValue();
            long tiempoInactivo = ahora.toEpochMilli() - ultimaAct.toEpochMilli();
            if (tiempoInactivo > EXPIRACION_MILLIS) {
                limpiarEstado(userId);
                limpiadas++;
            }
        }

        if (limpiadas > 0) {
            LOG.info("Se limpiaron {} sesiones expiradas", limpiadas);
        }
    }
}

