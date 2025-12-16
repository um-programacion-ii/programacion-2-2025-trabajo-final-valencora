package com.um.eventosproxy.web.rest;

import com.um.eventosproxy.dto.BloqueoAsientosRequestDTO;
import com.um.eventosproxy.service.CatedraVentaService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
public class VentaResource {

    private static final Logger LOG = LoggerFactory.getLogger(VentaResource.class);

    private final CatedraVentaService catedraVentaService;

    public VentaResource(CatedraVentaService catedraVentaService) {
        this.catedraVentaService = catedraVentaService;
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarVenta(@Valid @RequestBody Map<String, Object> request) {
        LOG.debug("REST request para confirmar venta: eventoId={}", request.get("eventoId"));
        
        try {
            // Convertir el request a BloqueoAsientosRequestDTO
            BloqueoAsientosRequestDTO bloqueoRequest = new BloqueoAsientosRequestDTO();
            bloqueoRequest.setEventoId(((Number) request.get("eventoId")).longValue());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> asientosRequest = (List<Map<String, Object>>) request.get("asientos");
            List<BloqueoAsientosRequestDTO.AsientoBloqueoDTO> asientos = new ArrayList<>();
            List<Map<String, String>> nombresPersonas = new ArrayList<>();
            
            for (Map<String, Object> asientoMap : asientosRequest) {
                BloqueoAsientosRequestDTO.AsientoBloqueoDTO asiento = new BloqueoAsientosRequestDTO.AsientoBloqueoDTO();
                // Convertir fila de String a Integer si es necesario
                Object filaObj = asientoMap.get("fila");
                if (filaObj instanceof String) {
                    asiento.setFila(Integer.parseInt((String) filaObj));
                } else if (filaObj instanceof Number) {
                    asiento.setFila(((Number) filaObj).intValue());
                }
                // El número viene como "numero" pero el DTO espera "columna"
                Object numeroObj = asientoMap.get("numero");
                if (numeroObj instanceof Number) {
                    asiento.setColumna(((Number) numeroObj).intValue());
                }
                asientos.add(asiento);
                
                // Extraer nombres de personas
                Map<String, String> nombrePersona = new HashMap<>();
                nombrePersona.put("nombre", (String) asientoMap.get("nombrePersona"));
                nombrePersona.put("apellido", (String) asientoMap.get("apellidoPersona"));
                nombresPersonas.add(nombrePersona);
            }
            
            bloqueoRequest.setAsientos(asientos);
            
            // Extraer precioVenta si viene en el request (opcional)
            Double precioVenta = null;
            Object precioObj = request.get("precioVenta");
            if (precioObj != null) {
                if (precioObj instanceof Number) {
                    precioVenta = ((Number) precioObj).doubleValue();
                }
            }
            
            // Confirmar venta con la cátedra
            Map<String, Object> respuesta = catedraVentaService.confirmarVenta(bloqueoRequest, nombresPersonas, precioVenta);
            
            // Convertir respuesta al formato esperado por el backend
            Map<String, Object> responseDTO = new HashMap<>();
            responseDTO.put("ventaIdCatedra", respuesta.get("ventaId"));
            
            // Convertir resultado a String (puede venir como Boolean o String de la cátedra)
            Object resultadoObj = respuesta.get("resultado");
            String resultadoStr;
            if (resultadoObj instanceof Boolean) {
                resultadoStr = ((Boolean) resultadoObj) ? "EXITOSA" : "FALLIDA";
            } else if (resultadoObj instanceof String) {
                resultadoStr = (String) resultadoObj;
            } else {
                resultadoStr = "FALLIDA";
            }
            responseDTO.put("resultado", resultadoStr);
            
            responseDTO.put("mensaje", respuesta.get("mensaje"));
            
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            LOG.error("Error al procesar confirmación de venta", e);
            Map<String, Object> error = new HashMap<>();
            error.put("resultado", "FALLIDA");
            error.put("mensaje", "Error al procesar la venta: " + e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
}

