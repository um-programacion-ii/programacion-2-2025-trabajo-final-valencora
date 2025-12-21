package com.um.eventosbackend.service;

import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.service.dto.EventoDetalleDTO;
import com.um.eventosbackend.service.dto.EventoResumenDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import com.um.eventosbackend.service.mapper.EventoMapper;
import com.um.eventosbackend.service.proxy.ProxyAsientosService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de consulta de eventos (listado y detalle).
 */
@Service
@Transactional(readOnly = true)
public class EventoQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(EventoQueryService.class);

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;
    private final ProxyAsientosService proxyAsientosService;

    public EventoQueryService(
        EventoRepository eventoRepository,
        EventoMapper eventoMapper,
        ProxyAsientosService proxyAsientosService
    ) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
        this.proxyAsientosService = proxyAsientosService;
    }

    /**
     * Obtiene el listado de eventos activos (no cancelados y no expirados).
     */
    public List<EventoResumenDTO> obtenerEventosActivos() {
        Instant ahora = Instant.now();
        List<Evento> eventos = eventoRepository.findEventosActivos(ahora);
        return eventos.stream().map(eventoMapper::toResumenDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle de un evento por ID, si está activo (no cancelado y no expirado).
     * Incluye estadísticas de asientos bloqueados.
     */
    public Optional<EventoDetalleDTO> obtenerDetalleEvento(Long id) {
        Instant ahora = Instant.now();
        Optional<EventoDetalleDTO> detalleOpt = eventoRepository
            .findById(id)
            .filter(e -> Boolean.FALSE.equals(e.getCancelado()))
            .filter(e -> e.getFecha() == null || !e.getFecha().isBefore(ahora))
            .map(eventoMapper::toDetalleDTO);

        // No incluir asientos en el detalle del evento (no establecer la lista)

        return detalleOpt;
    }

    /**
     * Obtiene la lista de asientos bloqueados en el formato requerido (fila, columna, estado).
     */
    private List<EventoDetalleDTO.AsientoBloqueadoDTO> obtenerAsientosBloqueados(MapaAsientosDTO mapaAsientos) {
        List<EventoDetalleDTO.AsientoBloqueadoDTO> asientosBloqueados = new java.util.ArrayList<>();
        
        if (mapaAsientos == null || mapaAsientos.getAsientos() == null || mapaAsientos.getAsientos().isEmpty()) {
            LOG.debug("Mapa de asientos vacío o nulo");
            return asientosBloqueados;
        }

        LOG.debug("Procesando {} asientos del mapa", mapaAsientos.getAsientos().size());
        int bloqueadosEncontrados = 0;

        for (MapaAsientosDTO.AsientoDTO asiento : mapaAsientos.getAsientos()) {
            LOG.debug("Procesando asiento: fila={}, numero={}, estado={}", 
                asiento.getFila(), asiento.getNumero(), asiento.getEstado());
            
            // Solo incluir asientos bloqueados
            boolean esBloqueado = false;
            if (asiento.getEstado() != null) {
                // Verificar si es el enum BLOQUEADO
                if (asiento.getEstado() == MapaAsientosDTO.AsientoDTO.EstadoAsiento.BLOQUEADO) {
                    esBloqueado = true;
                }
                // También verificar si viene como string (por si acaso)
                else {
                    String estadoStr = asiento.getEstado().toString();
                    if (estadoStr != null && estadoStr.equalsIgnoreCase("BLOQUEADO")) {
                        esBloqueado = true;
                    }
                }
            }
            
            if (esBloqueado) {
                bloqueadosEncontrados++;
                EventoDetalleDTO.AsientoBloqueadoDTO bloqueado = new EventoDetalleDTO.AsientoBloqueadoDTO();
                
                // Convertir fila de String a Integer
                try {
                    if (asiento.getFila() != null && !asiento.getFila().trim().isEmpty()) {
                        bloqueado.setFila(Integer.parseInt(asiento.getFila()));
                    } else {
                        LOG.warn("Fila es null o vacía para asiento bloqueado: numero={}", asiento.getNumero());
                        continue;
                    }
                } catch (NumberFormatException e) {
                    LOG.warn("No se pudo convertir fila '{}' a número para asiento numero={}: {}", 
                        asiento.getFila(), asiento.getNumero(), e.getMessage());
                    continue; // Saltar este asiento si no se puede convertir
                }
                
                // Usar numero como columna
                if (asiento.getNumero() != null) {
                    bloqueado.setColumna(asiento.getNumero());
                } else {
                    LOG.warn("Numero es null para asiento bloqueado: fila={}", asiento.getFila());
                    continue;
                }
                
                // Convertir estado enum a string
                bloqueado.setEstado("Bloqueado");
                
                asientosBloqueados.add(bloqueado);
                LOG.debug("Asiento bloqueado agregado: fila={}, columna={}", bloqueado.getFila(), bloqueado.getColumna());
            }
        }

        LOG.debug("Total de asientos bloqueados procesados: {}", bloqueadosEncontrados);
        return asientosBloqueados;
    }

    /**
     * Obtiene las dimensiones (filas y columnas) de un evento por su ID de cátedra.
     */
    public Optional<java.util.Map<String, Integer>> obtenerDimensionesEvento(Long eventoIdCatedra) {
        LOG.debug("Obteniendo dimensiones del evento con eventoIdCatedra: {}", eventoIdCatedra);
        
        return eventoRepository.findByEventoIdCatedra(eventoIdCatedra)
            .map(evento -> {
                java.util.Map<String, Integer> dimensiones = new java.util.HashMap<>();
                if (evento.getFilaAsiento() != null && evento.getColumnAsiento() != null) {
                    dimensiones.put("filas", evento.getFilaAsiento());
                    dimensiones.put("columnas", evento.getColumnAsiento());
                    LOG.debug("Dimensiones encontradas: {} filas x {} columnas", 
                        evento.getFilaAsiento(), evento.getColumnAsiento());
                    return dimensiones;
                } else {
                    LOG.warn("Evento con eventoIdCatedra={} no tiene dimensiones definidas", eventoIdCatedra);
                    return null;
                }
            });
    }
}

