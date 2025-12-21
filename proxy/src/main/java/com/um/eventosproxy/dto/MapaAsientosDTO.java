package com.um.eventosproxy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MapaAsientosDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("eventoId")
    private Long eventoId;

    @JsonProperty("asientos")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<AsientoDTO> asientos;

    @JsonProperty("matriz")
    private List<String> matriz = new ArrayList<>();
}

