package com.um.eventosbackend.service.proxy;

import com.um.eventosbackend.config.ApplicationProperties;
import com.um.eventosbackend.service.dto.VentaRequestDTO;
import com.um.eventosbackend.service.dto.VentaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxyVentaService {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyVentaService.class);
    private static final String ENDPOINT_VENTA = "/api/ventas/confirmar";

    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;

    public ProxyVentaService(
        @org.springframework.beans.factory.annotation.Qualifier("proxyRestTemplate") RestTemplate restTemplate,
        ApplicationProperties applicationProperties
    ) {
        this.restTemplate = restTemplate;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Confirma una venta con el servicio de la cátedra a través del proxy.
     */
    public VentaResponseDTO confirmarVenta(VentaRequestDTO request, java.math.BigDecimal precioVenta) {
        String url = applicationProperties.getProxy().getBaseUrl() + ENDPOINT_VENTA;
        LOG.debug("Confirmando venta a través del proxy: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Crear un Map con el request y el precio calculado
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("eventoId", request.getEventoId());
            requestBody.put("asientos", request.getAsientos());
            if (precioVenta != null) {
                requestBody.put("precioVenta", precioVenta.doubleValue());
            }

            HttpEntity<java.util.Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<java.util.Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                java.util.Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                java.util.Map<String, Object> body = response.getBody();
                VentaResponseDTO dto = new VentaResponseDTO();
                
                if (body.get("ventaIdCatedra") != null) {
                    dto.setVentaIdCatedra(((Number) body.get("ventaIdCatedra")).longValue());
                }
                
                // La cátedra puede devolver resultado como Boolean o String
                Object resultadoObj = body.get("resultado");
                String resultado;
                if (resultadoObj instanceof Boolean) {
                    resultado = ((Boolean) resultadoObj) ? "EXITOSA" : "FALLIDA";
                } else if (resultadoObj instanceof String) {
                    resultado = (String) resultadoObj;
                } else {
                    resultado = "FALLIDA";
                }
                dto.setResultado(resultado);
                
                dto.setMensaje((String) body.get("mensaje"));
                
                LOG.info("Venta confirmada: ventaIdCatedra={}, resultado={}", dto.getVentaIdCatedra(), dto.getResultado());
                return dto;
            } else {
                LOG.warn("Respuesta vacía o no exitosa del proxy para confirmar venta");
                return crearRespuestaError("Error al comunicarse con el proxy");
            }
        } catch (Exception e) {
            LOG.error("Error al confirmar venta a través del proxy", e);
            return crearRespuestaError("Error al comunicarse con el proxy: " + e.getMessage());
        }
    }

    private VentaResponseDTO crearRespuestaError(String mensaje) {
        VentaResponseDTO response = new VentaResponseDTO();
        response.setResultado("FALLIDA");
        response.setMensaje(mensaje);
        return response;
    }
}

