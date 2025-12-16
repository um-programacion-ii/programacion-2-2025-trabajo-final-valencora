package com.um.eventosbackend.web.rest;

import com.um.eventosbackend.domain.User;
import com.um.eventosbackend.domain.Venta;
import com.um.eventosbackend.repository.UserRepository;
import com.um.eventosbackend.repository.VentaRepository;
import com.um.eventosbackend.security.SecurityUtils;
import com.um.eventosbackend.service.dto.VentaRequestDTO;
import com.um.eventosbackend.service.dto.VentaResponseDTO;
import com.um.eventosbackend.service.dto.VentaResumenDTO;
import com.um.eventosbackend.service.venta.VentaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("isAuthenticated()")
public class VentaResource {

    private static final Logger LOG = LoggerFactory.getLogger(VentaResource.class);

    private final VentaService ventaService;
    private final VentaRepository ventaRepository;
    private final UserRepository userRepository;

    public VentaResource(
        VentaService ventaService,
        VentaRepository ventaRepository,
        UserRepository userRepository
    ) {
        this.ventaService = ventaService;
        this.ventaRepository = ventaRepository;
        this.userRepository = userRepository;
    }

    /**
     * {@code POST  /ventas} : Procesa una nueva venta.
     *
     * @param request el DTO de la venta a procesar.
     * @return el {@link ResponseEntity} con status {@code 201 (Created)} y con el cuerpo de la venta procesada.
     */
    @PostMapping
    public ResponseEntity<VentaResponseDTO> procesarVenta(@Valid @RequestBody VentaRequestDTO request) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para procesar venta: eventoId={}, usuario={}", request.getEventoId(), userLogin);
        
        try {
            VentaResponseDTO respuesta = ventaService.procesarVenta(request, userLogin);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            LOG.warn("Error de validación al procesar venta: {}", e.getMessage());
            VentaResponseDTO error = new VentaResponseDTO();
            error.setResultado("FALLIDA");
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalStateException e) {
            LOG.warn("Error de estado al procesar venta: {}", e.getMessage());
            VentaResponseDTO error = new VentaResponseDTO();
            error.setResultado("FALLIDA");
            error.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            LOG.error("Error inesperado al procesar venta", e);
            VentaResponseDTO error = new VentaResponseDTO();
            error.setResultado("FALLIDA");
            error.setMensaje("Error interno al procesar la venta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * {@code GET  /ventas} : Obtiene todas las ventas del usuario autenticado.
     *
     * @return el {@link ResponseEntity} con status {@code 200 (OK)} y la lista de ventas resumidas.
     */
    @GetMapping
    public ResponseEntity<List<VentaResumenDTO>> obtenerVentas() {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para obtener ventas del usuario: {}", userLogin);
        
        Optional<User> userOpt = userRepository.findOneByLogin(userLogin);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        List<Venta> ventas = ventaRepository.findByUsuarioOrderByFechaVentaDesc(userOpt.get());
        List<VentaResumenDTO> ventasDTO = ventas.stream()
            .map(this::convertirAVentaResumenDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ventasDTO);
    }

    /**
     * {@code GET  /ventas/:id} : Obtiene el detalle de una venta específica.
     *
     * @param id el id de la venta.
     * @return el {@link ResponseEntity} con status {@code 200 (OK)} y el cuerpo de la venta, o con status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> obtenerVenta(@PathVariable Long id) {
        String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow();
        LOG.debug("REST request para obtener venta id: {}, usuario: {}", id, userLogin);
        
        Optional<User> userOpt = userRepository.findOneByLogin(userLogin);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Venta venta = ventaRepository.findByIdAndUsuario(id, userOpt.get());
        if (venta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        VentaResponseDTO dto = convertirAVentaResponseDTO(venta);
        return ResponseEntity.ok(dto);
    }

    /**
     * Convierte una entidad Venta a VentaResumenDTO.
     */
    private VentaResumenDTO convertirAVentaResumenDTO(Venta venta) {
        VentaResumenDTO dto = new VentaResumenDTO();
        dto.setId(venta.getId());
        dto.setEventoId(venta.getEventoId());
        dto.setFechaVenta(venta.getFechaVenta());
        dto.setPrecioVenta(venta.getPrecioVenta());
        dto.setResultado(venta.getResultado().name());
        dto.setCantidadAsientos(venta.getAsientos() != null ? venta.getAsientos().size() : 0);
        return dto;
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

