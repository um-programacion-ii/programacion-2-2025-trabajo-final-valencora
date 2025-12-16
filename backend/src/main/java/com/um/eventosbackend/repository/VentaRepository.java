package com.um.eventosbackend.repository;

import com.um.eventosbackend.domain.User;
import com.um.eventosbackend.domain.Venta;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Venta entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    
    /**
     * Encuentra todas las ventas de un usuario.
     */
    List<Venta> findByUsuarioOrderByFechaVentaDesc(User usuario);
    
    /**
     * Encuentra todas las ventas pendientes que necesitan reintento.
     */
    @Query("SELECT v FROM Venta v WHERE v.resultado = 'PENDIENTE' AND v.intentosReintento < :maxIntentos")
    List<Venta> findVentasPendientesParaReintento(@Param("maxIntentos") Integer maxIntentos);
    
    /**
     * Encuentra una venta por su ID y usuario.
     */
    @Query("SELECT v FROM Venta v WHERE v.id = :id AND v.usuario = :usuario")
    Venta findByIdAndUsuario(@Param("id") Long id, @Param("usuario") User usuario);
}

