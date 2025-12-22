package com.um.eventosbackend.service.mapper;

import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.domain.EventoTipo;
import com.um.eventosbackend.domain.Integrante;
import com.um.eventosbackend.repository.EventoTipoRepository;
import com.um.eventosbackend.service.dto.CatedraEventoDTO;
import com.um.eventosbackend.service.dto.CatedraIntegranteDTO;
import com.um.eventosbackend.service.dto.EventoDetalleDTO;
import com.um.eventosbackend.service.dto.EventoResumenDTO;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir DTOs del servicio de cátedra a entidades locales.
 */
@Component
public class EventoMapper {

    private static final Logger LOG = LoggerFactory.getLogger(EventoMapper.class);

    private final EventoTipoRepository eventoTipoRepository;

    public EventoMapper(EventoTipoRepository eventoTipoRepository) {
        this.eventoTipoRepository = eventoTipoRepository;
    }

    /**
     * Convierte un DTO del servicio de cátedra a una entidad Evento local.
     */
    public Evento toEntity(CatedraEventoDTO dto) {
        if (dto == null) {
            return null;
        }

        Evento evento = new Evento();
        evento.setEventoIdCatedra(dto.getId());
        evento.setTitulo(dto.getTitulo());
        evento.setDescripcion(dto.getDescripcion());
        evento.setResumen(dto.getResumen());
        evento.setFecha(dto.getFecha());
        evento.setDireccion(dto.getDireccion());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setPrecio(dto.getPrecio());
        evento.setCancelado(dto.getCancelado() != null ? dto.getCancelado() : false);
        evento.setFilaAsiento(dto.getFilaAsientos());
        evento.setColumnAsiento(dto.getColumnAsientos());

        // Mapear tipo de evento
        if (dto.getTipo() != null && dto.getTipo().getNombre() != null) {
            EventoTipo tipo = eventoTipoRepository
                .findByNombre(dto.getTipo().getNombre())
                .orElseGet(() -> {
                    // Crear nuevo tipo si no existe
                    EventoTipo nuevoTipo = new EventoTipo();
                    nuevoTipo.setNombre(dto.getTipo().getNombre());
                    nuevoTipo.setDescripcion(dto.getTipo().getDescripcion());
                    return eventoTipoRepository.save(nuevoTipo);
                });
            evento.setTipo(tipo);
        }

        // Mapear integrantes
        if (dto.getIntegrantes() != null) {
            dto.getIntegrantes().forEach(integranteDTO -> {
                Integrante integrante = toIntegranteEntity(integranteDTO);
                evento.addIntegrante(integrante);
            });
        }

        return evento;
    }

    /**
     * Actualiza una entidad Evento existente con datos del DTO.
     */
    public void updateEntityFromDTO(Evento evento, CatedraEventoDTO dto) {
        if (dto == null || evento == null) {
            return;
        }

        evento.setTitulo(dto.getTitulo());
        evento.setDescripcion(dto.getDescripcion());
        evento.setResumen(dto.getResumen());
        evento.setFecha(dto.getFecha());
        evento.setDireccion(dto.getDireccion());
        evento.setImagenUrl(dto.getImagenUrl());
        evento.setPrecio(dto.getPrecio());
        evento.setCancelado(dto.getCancelado() != null ? dto.getCancelado() : false);
        evento.setFilaAsiento(dto.getFilaAsientos());
        evento.setColumnAsiento(dto.getColumnAsientos());

        // Actualizar tipo de evento
        if (dto.getTipo() != null && dto.getTipo().getNombre() != null) {
            EventoTipo tipo = eventoTipoRepository
                .findByNombre(dto.getTipo().getNombre())
                .orElseGet(() -> {
                    EventoTipo nuevoTipo = new EventoTipo();
                    nuevoTipo.setNombre(dto.getTipo().getNombre());
                    nuevoTipo.setDescripcion(dto.getTipo().getDescripcion());
                    return eventoTipoRepository.save(nuevoTipo);
                });
            evento.setTipo(tipo);
        }

        // Limpiar integrantes existentes y agregar nuevos
        evento.getIntegrantes().clear();
        if (dto.getIntegrantes() != null) {
            dto.getIntegrantes().forEach(integranteDTO -> {
                Integrante integrante = toIntegranteEntity(integranteDTO);
                evento.addIntegrante(integrante);
            });
        }
    }

    private Integrante toIntegranteEntity(CatedraIntegranteDTO dto) {
        Integrante integrante = new Integrante();
        integrante.setNombre(dto.getNombre());
        integrante.setDescripcion(dto.getDescripcion());
        integrante.setImagenUrl(dto.getImagenUrl());
        return integrante;
    }

    // ============================
    // Mapping a DTOs de respuesta
    // ============================

    public EventoResumenDTO toResumenDTO(Evento evento) {
        if (evento == null) {
            return null;
        }
        EventoResumenDTO dto = new EventoResumenDTO();
        dto.setId(evento.getId());
        dto.setTitulo(evento.getTitulo());
        dto.setResumen(evento.getResumen());
        dto.setFecha(evento.getFecha());
        dto.setDireccion(evento.getDireccion());
        dto.setPrecio(evento.getPrecio());
        dto.setCancelado(evento.getCancelado());
        return dto;
    }

    public EventoDetalleDTO toDetalleDTO(Evento evento) {
        if (evento == null) {
            return null;
        }
        EventoDetalleDTO dto = new EventoDetalleDTO();
        dto.setId(evento.getId());
        dto.setEventoIdCatedra(evento.getEventoIdCatedra());
        dto.setTitulo(evento.getTitulo());
        dto.setDescripcion(evento.getDescripcion());
        dto.setResumen(evento.getResumen());
        dto.setFecha(evento.getFecha());
        dto.setDireccion(evento.getDireccion());
        dto.setImagenUrl(evento.getImagenUrl());
        dto.setPrecio(evento.getPrecio());
        dto.setCancelado(evento.getCancelado());
        dto.setFilaAsientos(evento.getFilaAsiento());
        dto.setColumnAsientos(evento.getColumnAsiento());

        if (evento.getTipo() != null) {
            dto.setTipoNombre(evento.getTipo().getNombre());
            dto.setTipoDescripcion(evento.getTipo().getDescripcion());
        }

        if (evento.getIntegrantes() != null) {
            dto.setIntegrantes(
                evento
                    .getIntegrantes()
                    .stream()
                    .map(this::toDetalleIntegranteDTO)
                    .collect(java.util.stream.Collectors.toList())
            );
        }
        return dto;
    }

    private EventoDetalleDTO.IntegranteDTO toDetalleIntegranteDTO(Integrante integrante) {
        EventoDetalleDTO.IntegranteDTO dto = new EventoDetalleDTO.IntegranteDTO();
        dto.setNombre(integrante.getNombre());
        dto.setDescripcion(integrante.getDescripcion());
        dto.setImagenUrl(integrante.getImagenUrl());
        return dto;
    }
}
