package com.um.eventosbackend.service.asientos;

import com.um.eventosbackend.service.dto.BloqueoAsientosRequestDTO;
import com.um.eventosbackend.service.dto.BloqueoAsientosResponseDTO;
import com.um.eventosbackend.service.dto.EstadoSeleccionDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import com.um.eventosbackend.service.proxy.ProxyAsientosService;
import com.um.eventosbackend.service.sesion.SesionSeleccionService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(AsientosService.class);

    private final ProxyAsientosService proxyAsientosService;
    private final SesionSeleccionService sesionSeleccionService;

    public AsientosService(
        ProxyAsientosService proxyAsientosService,
        SesionSeleccionService sesionSeleccionService
    ) {
        this.proxyAsientosService = proxyAsientosService;
        this.sesionSeleccionService = sesionSeleccionService;
    }

    /**
     * Obtiene el mapa de asientos de un evento, incluyendo información de asientos seleccionados por el usuario.
     */
    public MapaAsientosDTO obtenerMapaAsientos(Long eventoId, String userId) {
        LOG.debug("Obteniendo mapa de asientos para eventoId: {}, userId: {}", eventoId, userId);

        // Obtener mapa desde el proxy (Redis de la cátedra)
        MapaAsientosDTO mapa = proxyAsientosService.obtenerMapaAsientos(eventoId);

        // Obtener asientos seleccionados por el usuario
        EstadoSeleccionDTO estadoSeleccion = sesionSeleccionService.obtenerEstado(userId);
        if (estadoSeleccion != null && estadoSeleccion.getEventoId() != null && estadoSeleccion.getEventoId().equals(eventoId)) {
            // Marcar asientos seleccionados por el usuario
            List<String> asientosSeleccionados = estadoSeleccion.getAsientosSeleccionados()
                .stream()
                .map(a -> a.getFila() + "-" + a.getNumero())
                .collect(Collectors.toList());

            if (mapa.getAsientos() != null) {
                mapa.getAsientos().forEach(asiento -> {
                    String clave = asiento.getFila() + "-" + asiento.getNumero();
                    if (asientosSeleccionados.contains(clave)) {
                        asiento.setSeleccionado(true);
                    }
                });
            }
        }

        return mapa;
    }

    /**
     * Bloquea temporalmente los asientos seleccionados.
     */
    public BloqueoAsientosResponseDTO bloquearAsientos(Long eventoId, String userId) {
        LOG.debug("Bloqueando asientos para eventoId: {}, userId: {}", eventoId, userId);

        // Obtener asientos seleccionados de la sesión
        EstadoSeleccionDTO estadoSeleccion = sesionSeleccionService.obtenerEstado(userId);
        if (estadoSeleccion == null || estadoSeleccion.getAsientosSeleccionados() == null || estadoSeleccion.getAsientosSeleccionados().isEmpty()) {
            BloqueoAsientosResponseDTO error = new BloqueoAsientosResponseDTO();
            error.setExitoso(false);
            error.setMensaje("No hay asientos seleccionados para bloquear");
            return error;
        }

        // Verificar que el evento coincida
        if (estadoSeleccion.getEventoId() == null || !estadoSeleccion.getEventoId().equals(eventoId)) {
            BloqueoAsientosResponseDTO error = new BloqueoAsientosResponseDTO();
            error.setExitoso(false);
            error.setMensaje("El evento seleccionado no coincide");
            return error;
        }

        // Preparar request de bloqueo
        BloqueoAsientosRequestDTO request = new BloqueoAsientosRequestDTO();
        request.setEventoId(eventoId);
        request.setAsientos(
            estadoSeleccion.getAsientosSeleccionados().stream()
                .map(a -> {
                    BloqueoAsientosRequestDTO.AsientoBloqueoDTO dto = new BloqueoAsientosRequestDTO.AsientoBloqueoDTO();
                    dto.setFila(a.getFila());
                    dto.setNumero(a.getNumero());
                    return dto;
                })
                .collect(Collectors.toList())
        );

        // Enviar solicitud de bloqueo al proxy
        return proxyAsientosService.bloquearAsientos(request);
    }
}

