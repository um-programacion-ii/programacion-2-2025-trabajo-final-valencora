package com.um.eventosbackend.service.venta;

import com.um.eventosbackend.domain.AsientoVenta;
import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.domain.User;
import com.um.eventosbackend.domain.Venta;
import com.um.eventosbackend.domain.Venta.ResultadoVenta;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.repository.UserRepository;
import com.um.eventosbackend.repository.VentaRepository;
import com.um.eventosbackend.service.dto.EstadoSeleccionDTO;
import com.um.eventosbackend.service.dto.VentaRequestDTO;
import com.um.eventosbackend.service.dto.VentaResponseDTO;
import com.um.eventosbackend.service.proxy.ProxyVentaService;
import com.um.eventosbackend.service.sesion.SesionSeleccionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VentaService {

    private static final Logger LOG = LoggerFactory.getLogger(VentaService.class);
    private static final int MAX_INTENTOS_REINTENTO = 5;

    private final VentaRepository ventaRepository;
    private final EventoRepository eventoRepository;
    private final UserRepository userRepository;
    private final ProxyVentaService proxyVentaService;
    private final SesionSeleccionService sesionSeleccionService;

    public VentaService(
        VentaRepository ventaRepository,
        EventoRepository eventoRepository,
        UserRepository userRepository,
        ProxyVentaService proxyVentaService,
        SesionSeleccionService sesionSeleccionService
    ) {
        this.ventaRepository = ventaRepository;
        this.eventoRepository = eventoRepository;
        this.userRepository = userRepository;
        this.proxyVentaService = proxyVentaService;
        this.sesionSeleccionService = sesionSeleccionService;
    }

    /**
     * Procesa una venta completa: valida, registra localmente y confirma con la cátedra.
     */
    public VentaResponseDTO procesarVenta(VentaRequestDTO request, String userLogin) {
        LOG.info("Procesando venta para eventoId: {}, usuario: {}", request.getEventoId(), userLogin);

        // Obtener usuario
        Optional<User> userOpt = userRepository.findOneByLogin(userLogin);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado: " + userLogin);
        }
        User usuario = userOpt.get();

        // Obtener evento
        Optional<Evento> eventoOpt = eventoRepository.findByEventoIdCatedra(request.getEventoId());
        if (eventoOpt.isEmpty()) {
            throw new IllegalArgumentException("Evento no encontrado: " + request.getEventoId());
        }
        Evento evento = eventoOpt.get();

        // Validar que el evento no esté cancelado o expirado
        if (evento.getCancelado()) {
            throw new IllegalStateException("El evento está cancelado");
        }
        if (evento.isExpirado()) {
            throw new IllegalStateException("El evento está expirado");
        }

        // Validar que hay asientos seleccionados
        if (request.getAsientos() == null || request.getAsientos().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un asiento");
        }

        // Obtener estado de selección para validar que los asientos estén bloqueados
        EstadoSeleccionDTO estadoSeleccion = sesionSeleccionService.obtenerEstado(userLogin);
        if (estadoSeleccion == null || estadoSeleccion.getEventoId() == null || !estadoSeleccion.getEventoId().equals(request.getEventoId())) {
            throw new IllegalStateException("No hay asientos bloqueados para este evento");
        }

        // Calcular precio total
        BigDecimal precioTotal = evento.getPrecio() != null 
            ? evento.getPrecio().multiply(BigDecimal.valueOf(request.getAsientos().size()))
            : BigDecimal.ZERO;

        // Crear venta local
        Venta venta = new Venta();
        venta.setEventoId(evento.getId());
        venta.setUsuario(usuario);
        venta.setFechaVenta(Instant.now());
        venta.setPrecioVenta(precioTotal);
        venta.setResultado(ResultadoVenta.PENDIENTE);
        venta.setMensaje("Venta pendiente de confirmación");

        // Agregar asientos a la venta
        for (VentaRequestDTO.AsientoVentaDTO asientoDTO : request.getAsientos()) {
            AsientoVenta asientoVenta = new AsientoVenta();
            asientoVenta.setVenta(venta);
            asientoVenta.setFila(asientoDTO.getFila());
            asientoVenta.setNumero(asientoDTO.getNumero());
            asientoVenta.setNombrePersona(asientoDTO.getNombrePersona());
            asientoVenta.setApellidoPersona(asientoDTO.getApellidoPersona());
            venta.addAsiento(asientoVenta);
        }

        // Guardar venta localmente
        venta = ventaRepository.save(venta);
        LOG.info("Venta local creada con id: {}", venta.getId());

        // Intentar confirmar con la cátedra
        try {
            VentaResponseDTO respuestaCatedra = proxyVentaService.confirmarVenta(request, precioTotal);
            
            if ("EXITOSA".equals(respuestaCatedra.getResultado()) || "EXITOSO".equals(respuestaCatedra.getResultado())) {
                // Venta exitosa
                venta.setResultado(ResultadoVenta.EXITOSA);
                venta.setVentaIdCatedra(respuestaCatedra.getVentaIdCatedra());
                venta.setMensaje(respuestaCatedra.getMensaje() != null ? respuestaCatedra.getMensaje() : "Venta confirmada exitosamente");
                ventaRepository.save(venta);
                
                // Limpiar sesión de selección
                sesionSeleccionService.limpiarEstado(userLogin);
                
                LOG.info("Venta confirmada exitosamente: ventaId={}, ventaIdCatedra={}", venta.getId(), venta.getVentaIdCatedra());
            } else {
                // Venta fallida
                venta.setResultado(ResultadoVenta.FALLIDA);
                venta.setMensaje(respuestaCatedra.getMensaje() != null ? respuestaCatedra.getMensaje() : "Error al confirmar venta con la cátedra");
                ventaRepository.save(venta);
                
                LOG.warn("Venta fallida: ventaId={}, mensaje={}", venta.getId(), venta.getMensaje());
            }
            
            return convertirAVentaResponseDTO(venta);
        } catch (Exception e) {
            // Error de comunicación - marcar como pendiente para reintento
            venta.setResultado(ResultadoVenta.PENDIENTE);
            venta.setMensaje("Error al comunicarse con la cátedra: " + e.getMessage());
            venta.setIntentosReintento(0);
            ventaRepository.save(venta);
            
            LOG.error("Error al confirmar venta con la cátedra, marcada como pendiente: ventaId={}", venta.getId(), e);
            
            VentaResponseDTO respuesta = convertirAVentaResponseDTO(venta);
            respuesta.setResultado("PENDIENTE");
            respuesta.setMensaje("Venta registrada localmente, pendiente de confirmación");
            return respuesta;
        }
    }

    /**
     * Reintenta confirmar una venta pendiente con la cátedra.
     */
    public boolean reintentarVenta(Venta venta) {
        LOG.info("Reintentando venta id: {}, intentos previos: {}", venta.getId(), venta.getIntentosReintento());

        if (venta.getIntentosReintento() >= MAX_INTENTOS_REINTENTO) {
            venta.setResultado(ResultadoVenta.FALLIDA);
            venta.setMensaje("Venta fallida después de " + MAX_INTENTOS_REINTENTO + " intentos");
            ventaRepository.save(venta);
            LOG.warn("Venta marcada como fallida después de {} intentos: ventaId={}", MAX_INTENTOS_REINTENTO, venta.getId());
            return false;
        }

        // Obtener evento para calcular precio
        Optional<Evento> eventoOpt = eventoRepository.findById(venta.getEventoId());
        if (eventoOpt.isEmpty()) {
            LOG.warn("Evento no encontrado para reintentar venta: eventoId={}", venta.getEventoId());
            return false;
        }
        Evento evento = eventoOpt.get();
        
        // Calcular precio total
        BigDecimal precioTotal = evento.getPrecio() != null 
            ? evento.getPrecio().multiply(BigDecimal.valueOf(venta.getAsientos().size()))
            : BigDecimal.ZERO;

        // Preparar request para la cátedra
        VentaRequestDTO request = new VentaRequestDTO();
        request.setEventoId(evento.getEventoIdCatedra()); // Usar eventoIdCatedra, no el ID interno
        request.setAsientos(
            venta.getAsientos().stream()
                .map(a -> {
                    VentaRequestDTO.AsientoVentaDTO dto = new VentaRequestDTO.AsientoVentaDTO();
                    dto.setFila(a.getFila());
                    dto.setNumero(a.getNumero());
                    dto.setNombrePersona(a.getNombrePersona());
                    dto.setApellidoPersona(a.getApellidoPersona());
                    return dto;
                })
                .collect(Collectors.toList())
        );

        try {
            venta.setUltimoIntentoReintento(Instant.now());
            venta.setIntentosReintento(venta.getIntentosReintento() + 1);
            ventaRepository.save(venta);

            VentaResponseDTO respuestaCatedra = proxyVentaService.confirmarVenta(request, precioTotal);
            
            if ("EXITOSA".equals(respuestaCatedra.getResultado()) || "EXITOSO".equals(respuestaCatedra.getResultado())) {
                venta.setResultado(ResultadoVenta.EXITOSA);
                venta.setVentaIdCatedra(respuestaCatedra.getVentaIdCatedra());
                venta.setMensaje(respuestaCatedra.getMensaje() != null ? respuestaCatedra.getMensaje() : "Venta confirmada exitosamente");
                ventaRepository.save(venta);
                
                LOG.info("Venta confirmada exitosamente en reintento: ventaId={}, ventaIdCatedra={}", venta.getId(), venta.getVentaIdCatedra());
                return true;
            } else {
                LOG.warn("Reintento fallido para venta id: {}, intento: {}", venta.getId(), venta.getIntentosReintento());
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error en reintento de venta id: {}", venta.getId(), e);
            return false;
        }
    }

    /**
     * Convierte una entidad Venta a VentaResponseDTO.
     */
    private VentaResponseDTO convertirAVentaResponseDTO(Venta venta) {
        VentaResponseDTO dto = new VentaResponseDTO();
        dto.setId(venta.getId());
        dto.setVentaIdCatedra(venta.getVentaIdCatedra());
        dto.setEventoId(venta.getEventoId());
        dto.setFechaVenta(venta.getFechaVenta());
        dto.setPrecioVenta(venta.getPrecioVenta());
        dto.setResultado(venta.getResultado().name());
        dto.setMensaje(venta.getMensaje());
        
        dto.setAsientos(
            venta.getAsientos().stream()
                .map(a -> {
                    VentaResponseDTO.AsientoVentaDTO asientoDTO = new VentaResponseDTO.AsientoVentaDTO();
                    asientoDTO.setFila(a.getFila());
                    asientoDTO.setNumero(a.getNumero());
                    asientoDTO.setNombrePersona(a.getNombrePersona());
                    asientoDTO.setApellidoPersona(a.getApellidoPersona());
                    return asientoDTO;
                })
                .collect(Collectors.toList())
        );
        
        return dto;
    }
}

