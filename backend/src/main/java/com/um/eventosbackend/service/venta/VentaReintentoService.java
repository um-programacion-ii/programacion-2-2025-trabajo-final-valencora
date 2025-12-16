package com.um.eventosbackend.service.venta;

import com.um.eventosbackend.domain.Venta;
import com.um.eventosbackend.repository.VentaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para reintentar ventas pendientes automáticamente.
 */
@Service
public class VentaReintentoService {

    private static final Logger LOG = LoggerFactory.getLogger(VentaReintentoService.class);
    private static final int MAX_INTENTOS_REINTENTO = 5;

    private final VentaRepository ventaRepository;
    private final VentaService ventaService;

    public VentaReintentoService(VentaRepository ventaRepository, VentaService ventaService) {
        this.ventaRepository = ventaRepository;
        this.ventaService = ventaService;
    }

    /**
     * Reintenta automáticamente las ventas pendientes.
     * Se ejecuta cada 2 minutos.
     */
    @Scheduled(fixedRate = 120000) // Cada 2 minutos
    @Transactional
    public void reintentarVentasPendientes() {
        LOG.debug("Iniciando proceso de reintento de ventas pendientes");
        
        List<Venta> ventasPendientes = ventaRepository.findVentasPendientesParaReintento(MAX_INTENTOS_REINTENTO);
        
        if (ventasPendientes.isEmpty()) {
            LOG.debug("No hay ventas pendientes para reintentar");
            return;
        }

        LOG.info("Encontradas {} ventas pendientes para reintentar", ventasPendientes.size());
        
        int exitosas = 0;
        int fallidas = 0;
        
        for (Venta venta : ventasPendientes) {
            try {
                boolean exitosa = ventaService.reintentarVenta(venta);
                if (exitosa) {
                    exitosas++;
                } else {
                    fallidas++;
                }
            } catch (Exception e) {
                LOG.error("Error al reintentar venta id: {}", venta.getId(), e);
                fallidas++;
            }
        }
        
        LOG.info("Proceso de reintento completado: {} exitosas, {} fallidas", exitosas, fallidas);
    }
}

