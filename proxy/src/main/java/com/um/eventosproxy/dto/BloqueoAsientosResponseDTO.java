package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BloqueoAsientosResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // La cátedra devuelve "resultado" en lugar de "exitoso"
    @JsonProperty("resultado")
    private Boolean resultado;
    
    // También acepta "exitoso" por compatibilidad
    @JsonProperty("exitoso")
    private Boolean exitoso;

    // La cátedra devuelve "descripcion" en lugar de "mensaje"
    @JsonProperty("descripcion")
    private String descripcion;
    
    // También acepta "mensaje" por compatibilidad
    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("eventoId")
    private Long eventoId;

    // La cátedra devuelve "asientos" con estados, no separados en bloqueados/no disponibles
    @JsonProperty("asientos")
    private List<AsientoConEstadoDTO> asientos;

    @JsonProperty("asientosBloqueados")
    private List<AsientoBloqueoDTO> asientosBloqueados;

    @JsonProperty("asientosNoDisponibles")
    private List<AsientoBloqueoDTO> asientosNoDisponibles;
    
    // Método helper para obtener el éxito (compatibilidad con ambos formatos)
    public Boolean obtenerExitoso() {
        return resultado != null ? resultado : (exitoso != null ? exitoso : false);
    }
    
    // Método helper para obtener el mensaje (compatibilidad con ambos formatos)
    public String obtenerMensaje() {
        return descripcion != null ? descripcion : (mensaje != null ? mensaje : "");
    }

    @Data
    public static class AsientoBloqueoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private Integer fila;

        @JsonProperty("columna")
        private Integer columna;
    }
    
    @Data
    public static class AsientoConEstadoDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fila")
        private Integer fila;

        @JsonProperty("columna")
        private Integer columna;
        
        @JsonProperty("estado")
        private String estado;
    }
}

