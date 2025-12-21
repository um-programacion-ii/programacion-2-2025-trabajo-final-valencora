package com.um.eventosproxy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosproxy.dto.AsientoDTO;
import com.um.eventosproxy.dto.MapaAsientosDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAsientosService {

    private static final Logger LOG = LoggerFactory.getLogger(RedisAsientosService.class);
    // Formato real de las claves en Redis según la cátedra y el enunciado: "evento_{id}"
    // El valor es un JSON con "eventoId" y "asientos"
    private static final String REDIS_KEY_PREFIX = "evento_";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CatedraAsientosService catedraAsientosService;
    private final BackendAsientosService backendAsientosService;

    public RedisAsientosService(
        RedisTemplate<String, String> redisTemplate, 
        ObjectMapper objectMapper,
        CatedraAsientosService catedraAsientosService,
        BackendAsientosService backendAsientosService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.catedraAsientosService = catedraAsientosService;
        this.backendAsientosService = backendAsientosService;
    }

    public MapaAsientosDTO obtenerMapaAsientos(Long eventoId) {
        // La cátedra y la guía de la materia usan claves del tipo "evento_{id}"
        String key = REDIS_KEY_PREFIX + eventoId;
        LOG.info("=== CONSULTANDO ASIENTOS === eventoId: {}, buscando key en Redis: '{}'", eventoId, key);

        try {
            String data = redisTemplate.opsForValue().get(key);
            LOG.info("Resultado de Redis GET para key '{}': data es null={}, vacío={}, longitud={}", 
                key, data == null, data != null && data.isEmpty(), data != null ? data.length() : 0);
            
            if (data == null || data.isEmpty()) {
                LOG.warn("No se encontraron datos de asientos en Redis para eventoId: {}, key: {}", eventoId, key);
                // Si no hay datos en Redis, intentar obtener dimensiones del evento y generar matriz completa
                // La cátedra solo guarda asientos cuando hay bloqueos; si no existen, mostrar matriz con todos libres
                MapaAsientosDTO mapa = crearMapaVacio(eventoId);
                
                // Obtener dimensiones del evento desde el backend
                java.util.Map<String, Integer> dimensiones = backendAsientosService.obtenerDimensionesEvento(eventoId);
                
                // Generar representación matricial del mapa de asientos usando las dimensiones del evento
                generarMatrizAsientos(mapa, 
                    dimensiones != null ? dimensiones.get("filas") : null,
                    dimensiones != null ? dimensiones.get("columnas") : null);
                
                return mapa;
            }

            LOG.info("Datos obtenidos de Redis (primeros 1000 caracteres): {}", 
                data.length() > 1000 ? data.substring(0, 1000) + "..." : data);

            // Intentar parsear como objeto JSON primero
            List<AsientoDTO> asientos = new ArrayList<>();
            try {
                Object parsed = objectMapper.readValue(data, Object.class);
                LOG.info("Tipo de dato parseado de Redis: {}", parsed.getClass().getSimpleName());
                
                if (parsed instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rawData = (Map<String, Object>) parsed;
                    LOG.info("Datos parseados de Redis como Map: keys={}", rawData.keySet());
                    
                    // Intentar diferentes estructuras posibles
                    if (rawData.containsKey("asientos")) {
                        Object asientosObj = rawData.get("asientos");
                        if (asientosObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> asientosList = (List<Object>) asientosObj;
                            LOG.info("Encontrado campo 'asientos' con {} elementos", asientosList.size());
                            for (Object item : asientosList) {
                                if (item instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> asientoData = (Map<String, Object>) item;
                                    LOG.info("Procesando asiento desde Redis (dentro de Map.asientos): {}", asientoData);
                                    AsientoDTO asiento = parsearAsiento(asientoData);
                                    if (asiento != null) {
                                        asientos.add(asiento);
                                    }
                                }
                            }
                        }
                    } else {
                        // Si no hay campo "asientos", intentar parsear el Map directamente como si fuera un array de asientos
                        asientos = parsearAsientos(rawData);
                    }
                } else if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> asientosList = (List<Object>) parsed;
                    LOG.info("Datos parseados de Redis como List directo: {} elementos", asientosList.size());
                    for (Object item : asientosList) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> asientoData = (Map<String, Object>) item;
                            LOG.info("Procesando asiento desde Redis (List directo): {}", asientoData);
                            AsientoDTO asiento = parsearAsiento(asientoData);
                            if (asiento != null) {
                                asientos.add(asiento);
                            }
                        } else {
                            LOG.warn("Elemento en List no es un Map: {}", item.getClass());
                        }
                    }
                } else {
                    LOG.warn("Formato de datos en Redis no reconocido: {}", parsed.getClass());
                }
            } catch (Exception parseException) {
                LOG.error("Error al parsear datos de Redis: {}", parseException.getMessage(), parseException);
                LOG.error("Datos que causaron el error (primeros 500 chars): {}", 
                    data.length() > 500 ? data.substring(0, 500) + "..." : data);
                throw parseException;
            }
            
            LOG.info("Asientos parseados desde Redis: {} total, {} bloqueados", 
                asientos.size(),
                asientos.stream()
                    .filter(a -> a.getEstado() == AsientoDTO.EstadoAsiento.BLOQUEADO)
                    .count());

            MapaAsientosDTO mapa = new MapaAsientosDTO();
            mapa.setEventoId(eventoId);
            mapa.setAsientos(asientos);

            // Obtener dimensiones del evento desde el backend
            java.util.Map<String, Integer> dimensiones = backendAsientosService.obtenerDimensionesEvento(eventoId);
            
            // Generar representación matricial del mapa de asientos usando las dimensiones del evento
            generarMatrizAsientos(mapa, 
                dimensiones != null ? dimensiones.get("filas") : null,
                dimensiones != null ? dimensiones.get("columnas") : null);

            LOG.debug("Mapa de asientos obtenido: {} asientos para eventoId: {}", asientos.size(), eventoId);
            return mapa;

        } catch (Exception e) {
            LOG.error("Error al obtener mapa de asientos desde Redis para eventoId: {}, key: {}", eventoId, key, e);
            LOG.error("Stack trace completo:", e);
            // Ante cualquier error al leer/parsear Redis, intentar generar matriz desde dimensiones
            MapaAsientosDTO mapa = crearMapaVacio(eventoId);
            
            // Obtener dimensiones del evento desde el backend
            java.util.Map<String, Integer> dimensiones = backendAsientosService.obtenerDimensionesEvento(eventoId);
            
            // Generar representación matricial del mapa de asientos usando las dimensiones del evento
            generarMatrizAsientos(mapa, 
                dimensiones != null ? dimensiones.get("filas") : null,
                dimensiones != null ? dimensiones.get("columnas") : null);
            
            return mapa;
        }
    }

    private List<AsientoDTO> parsearAsientos(Map<String, Object> rawData) {
        List<AsientoDTO> asientos = new ArrayList<>();

        // Intentar parsear diferentes formatos posibles de Redis
        if (rawData.containsKey("asientos")) {
            Object asientosObj = rawData.get("asientos");
            if (asientosObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> asientosList = (List<Object>) asientosObj;
                LOG.info("Parseando {} asientos desde campo 'asientos'", asientosList.size());
                for (Object item : asientosList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> asientoData = (Map<String, Object>) item;
                        AsientoDTO asiento = parsearAsiento(asientoData);
                        if (asiento != null) {
                            asientos.add(asiento);
                        }
                    }
                }
            }
        } else {
            // Si no hay estructura "asientos", intentar parsear directamente
            // Puede ser que el Map contenga asientos como valores
            LOG.info("No se encontró campo 'asientos', intentando parsear Map directamente");
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> asientoData = (Map<String, Object>) entry.getValue();
                    AsientoDTO asiento = parsearAsiento(asientoData);
                    if (asiento != null) {
                        asientos.add(asiento);
                    }
                } else if (entry.getValue() instanceof List) {
                    // Puede ser que haya una lista de asientos en algún campo
                    @SuppressWarnings("unchecked")
                    List<Object> lista = (List<Object>) entry.getValue();
                    for (Object item : lista) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> asientoData = (Map<String, Object>) item;
                            AsientoDTO asiento = parsearAsiento(asientoData);
                            if (asiento != null) {
                                asientos.add(asiento);
                            }
                        }
                    }
                }
            }
        }

        return asientos;
    }

    private AsientoDTO parsearAsiento(Map<String, Object> data) {
        try {
            AsientoDTO asiento = new AsientoDTO();
            
            // Manejar fila: puede venir como String o Integer
            Object filaObj = data.getOrDefault("fila", "");
            if (filaObj instanceof Integer) {
                asiento.setFila(String.valueOf(filaObj));
            } else if (filaObj instanceof String) {
                asiento.setFila((String) filaObj);
            } else {
                asiento.setFila("");
            }
            
            // Manejar numero/columna: puede venir como "numero" o "columna"
            Object numeroObj = data.getOrDefault("numero", data.getOrDefault("columna", 0));
            if (numeroObj instanceof Integer) {
                asiento.setNumero((Integer) numeroObj);
            } else if (numeroObj instanceof Number) {
                asiento.setNumero(((Number) numeroObj).intValue());
            } else {
                asiento.setNumero(0);
            }

            // Manejar estado: puede venir como "Bloqueado", "BLOQUEADO", "Libre", "Ocupado", etc.
            Object estadoObj = data.get("estado");
            String estadoStr = null;
            if (estadoObj instanceof String) {
                estadoStr = (String) estadoObj;
            } else if (estadoObj != null) {
                estadoStr = estadoObj.toString();
            }
            
            if (estadoStr != null && !estadoStr.isEmpty()) {
                try {
                    // Normalizar el estado: "Bloqueado" -> "BLOQUEADO", "Libre" -> "LIBRE", "Ocupado" -> "OCUPADO"
                    String estadoNormalizado = estadoStr.toUpperCase().trim();
                    
                    // Mapear variaciones comunes
                    if (estadoNormalizado.equals("BLOQUEADO") || estadoNormalizado.equals("BLOQUEADOS") 
                        || estadoNormalizado.contains("BLOQUEO") || estadoNormalizado.equals("BLOQUEO EXITOSO")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.BLOQUEADO);
                    } else if (estadoNormalizado.equals("OCUPADO") || estadoNormalizado.equals("OCUPADOS") 
                        || estadoNormalizado.equals("VENDIDO") || estadoNormalizado.equals("VENDIDOS")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.OCUPADO);
                    } else if (estadoNormalizado.equals("LIBRE") || estadoNormalizado.equals("LIBRES") 
                        || estadoNormalizado.equals("DISPONIBLE") || estadoNormalizado.equals("DISPONIBLES")) {
                        asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                    } else {
                        // Intentar parsear directamente
                        try {
                            asiento.setEstado(AsientoDTO.EstadoAsiento.valueOf(estadoNormalizado));
                        } catch (IllegalArgumentException e) {
                            LOG.warn("Estado '{}' no reconocido después de todos los intentos, usando LIBRE", estadoStr);
                            asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOG.warn("Estado '{}' no reconocido después de normalizar, usando LIBRE por defecto", estadoStr);
                    asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
                }
            } else {
                asiento.setEstado(AsientoDTO.EstadoAsiento.LIBRE);
            }

            LOG.info("Asiento parseado desde Redis: fila={}, numero={}, estado={}", 
                asiento.getFila(), asiento.getNumero(), asiento.getEstado());
            
            return asiento;
        } catch (Exception e) {
            LOG.warn("Error al parsear asiento desde Redis: {}", data, e);
            return null;
        }
    }

    private MapaAsientosDTO crearMapaVacio(Long eventoId) {
        MapaAsientosDTO mapa = new MapaAsientosDTO();
        mapa.setEventoId(eventoId);
        mapa.setAsientos(new ArrayList<>());
        mapa.setMatriz(new ArrayList<>());
        return mapa;
    }

    /**
     * Genera una representación matricial visual del mapa de asientos.
     * Cada fila de la matriz representa una fila de asientos, y cada carácter representa un asiento:
     * - L = Libre
     * - O = Ocupado
     * - B = Bloqueado
     * 
     * @param mapa El mapa de asientos a representar
     * @param filasEvento Número de filas del evento (opcional, si es null se calcula desde los asientos)
     * @param columnasEvento Número de columnas del evento (opcional, si es null se calcula desde los asientos)
     */
    private void generarMatrizAsientos(MapaAsientosDTO mapa, Integer filasEvento, Integer columnasEvento) {
        // Si tenemos dimensiones del evento pero no hay asientos, generar matriz completa con todos libres
        if ((mapa.getAsientos() == null || mapa.getAsientos().isEmpty()) && filasEvento != null && columnasEvento != null) {
            LOG.info("No hay asientos pero tenemos dimensiones del evento ({}x{}), generando matriz completa con todos libres", 
                filasEvento, columnasEvento);
            generarMatrizCompletaDesdeDimensiones(mapa, filasEvento, columnasEvento);
            return;
        }
        
        // Si no hay asientos y tampoco dimensiones, retornar matriz vacía
        if (mapa.getAsientos() == null || mapa.getAsientos().isEmpty()) {
            mapa.setMatriz(new ArrayList<>());
            return;
        }

        // Crear un mapa para acceso rápido: fila -> (numero -> estado)
        java.util.Map<String, java.util.Map<Integer, Character>> mapaEstados = new java.util.HashMap<>();
        
        // También necesitamos saber el rango de filas y columnas
        int maxColumna = 0;
        int minColumna = Integer.MAX_VALUE;
        int maxFila = 0;
        int minFila = Integer.MAX_VALUE;
        java.util.Set<String> filasUnicas = new java.util.HashSet<>();

        for (AsientoDTO asiento : mapa.getAsientos()) {
            String fila = asiento.getFila();
            int columna = asiento.getNumero();
            
            filasUnicas.add(fila);
            maxColumna = Math.max(maxColumna, columna);
            minColumna = Math.min(minColumna, columna);
            
            // Intentar convertir fila a número para calcular rango
            try {
                int filaNum = Integer.parseInt(fila);
                maxFila = Math.max(maxFila, filaNum);
                minFila = Math.min(minFila, filaNum);
            } catch (NumberFormatException e) {
                // Si la fila no es numérica, no podemos calcular el rango
            }
            
            // Determinar estado del asiento
            char estado;
            if (asiento.getEstado() == null) {
                estado = 'L';
            } else {
                switch (asiento.getEstado()) {
                    case OCUPADO:
                        estado = 'O';
                        break;
                    case BLOQUEADO:
                        estado = 'B';
                        break;
                    case LIBRE:
                    default:
                        estado = 'L';
                        break;
                }
            }
            
            // Guardar en el mapa
            mapaEstados.computeIfAbsent(fila, k -> new java.util.HashMap<>())
                .put(columna, estado);
        }

        // Usar las dimensiones del evento si están disponibles, sino calcular desde los asientos
        int filasInicio = 1;
        int filasFin = filasEvento != null ? filasEvento : (minFila != Integer.MAX_VALUE && maxFila != 0 ? maxFila : 0);
        int columnasInicio = 1;
        int columnasFin = columnasEvento != null ? columnasEvento : (minColumna != Integer.MAX_VALUE && maxColumna != 0 ? maxColumna : 0);
        
        if (filasEvento != null && columnasEvento != null) {
            LOG.info("Usando dimensiones del evento: {} filas x {} columnas", filasEvento, columnasEvento);
            filasInicio = 1;
            filasFin = filasEvento;
            columnasInicio = 1;
            columnasFin = columnasEvento;
        } else {
            LOG.info("Rango calculado desde asientos: filas [{}, {}], columnas [{}, {}], total asientos: {}", 
                minFila != Integer.MAX_VALUE ? minFila : "N/A",
                maxFila != 0 ? maxFila : "N/A",
                minColumna != Integer.MAX_VALUE ? minColumna : "N/A",
                maxColumna != 0 ? maxColumna : "N/A",
                mapa.getAsientos().size());
            
            if (minFila != Integer.MAX_VALUE && maxFila != 0) {
                filasInicio = minFila;
                filasFin = maxFila;
            }
            if (minColumna != Integer.MAX_VALUE && maxColumna != 0) {
                columnasInicio = minColumna;
                columnasFin = maxColumna;
            }
        }

        // Si no hay asientos válidos y no tenemos dimensiones del evento, retornar matriz vacía
        if (mapaEstados.isEmpty() && filasEvento == null && columnasEvento == null) {
            mapa.setMatriz(new ArrayList<>());
            return;
        }

        // Generar la matriz
        List<String> matriz = new ArrayList<>();
        
        // Agregar encabezado con números de columnas
        if (columnasFin > 0) {
            StringBuilder encabezado = new StringBuilder("        "); // Espacio para "Fila X: "
            for (int col = columnasInicio; col <= columnasFin; col++) {
                encabezado.append(String.format("%-3d", col));
            }
            matriz.add(encabezado.toString());
            matriz.add(""); // Línea en blanco
        }
        
        // Generar todas las filas del rango si las filas son numéricas
        if (filasFin > 0 && columnasFin > 0) {
            LOG.info("Generando matriz completa: {} filas ({} a {}), {} columnas ({} a {})", 
                (filasFin - filasInicio + 1), filasInicio, filasFin,
                (columnasFin - columnasInicio + 1), columnasInicio, columnasFin);
            
            // Generar todas las filas desde filasInicio hasta filasFin
            for (int filaNum = filasInicio; filaNum <= filasFin; filaNum++) {
                String fila = String.valueOf(filaNum);
                java.util.Map<Integer, Character> asientosFila = mapaEstados.getOrDefault(fila, new java.util.HashMap<>());
                
                // Construir la línea de la matriz
                StringBuilder linea = new StringBuilder();
                linea.append("Fila ").append(String.format("%-3s", fila)).append(": ");
                
                // Crear una línea completa con todas las columnas del rango
                for (int col = columnasInicio; col <= columnasFin; col++) {
                    if (asientosFila.containsKey(col)) {
                        linea.append(String.format("%-3s", asientosFila.get(col)));
                    } else {
                        linea.append(String.format("%-3s", "L")); // Asiento no registrado, asumimos libre
                    }
                }
                
                matriz.add(linea.toString());
            }
        } else {
            // Si no podemos generar con dimensiones, usar el método anterior
            List<String> filasOrdenadas = new ArrayList<>(filasUnicas);
            filasOrdenadas.sort((f1, f2) -> {
                try {
                    int n1 = Integer.parseInt(f1);
                    int n2 = Integer.parseInt(f2);
                    return Integer.compare(n1, n2);
                } catch (NumberFormatException e) {
                    return f1.compareTo(f2);
                }
            });

            for (String fila : filasOrdenadas) {
                java.util.Map<Integer, Character> asientosFila = mapaEstados.getOrDefault(fila, new java.util.HashMap<>());
                StringBuilder linea = new StringBuilder();
                linea.append("Fila ").append(String.format("%-3s", fila)).append(": ");
                
                if (minColumna != Integer.MAX_VALUE && maxColumna != 0) {
                    for (int col = minColumna; col <= maxColumna; col++) {
                        if (asientosFila.containsKey(col)) {
                            linea.append(String.format("%-3s", asientosFila.get(col)));
                        } else {
                            linea.append(String.format("%-3s", "L"));
                        }
                    }
                } else {
                    List<Integer> columnasOrdenadas = new ArrayList<>(asientosFila.keySet());
                    columnasOrdenadas.sort(Integer::compareTo);
                    for (int col : columnasOrdenadas) {
                        linea.append(String.format("%-3s", asientosFila.get(col)));
                    }
                }
                matriz.add(linea.toString());
            }
        }

        // Agregar leyenda al final
        matriz.add("");
        matriz.add("Leyenda: L = Libre, O = Ocupado, B = Bloqueado");

        mapa.setMatriz(matriz);
        LOG.debug("Matriz de asientos generada: {} líneas", matriz.size());
    }

    /**
     * Genera una matriz completa desde las dimensiones del evento, marcando todos los asientos como libres.
     * Se usa cuando no hay asientos registrados en Redis pero conocemos las dimensiones del evento.
     * 
     * @param mapa El mapa de asientos a completar
     * @param filasEvento Número de filas del evento
     * @param columnasEvento Número de columnas del evento
     */
    private void generarMatrizCompletaDesdeDimensiones(MapaAsientosDTO mapa, Integer filasEvento, Integer columnasEvento) {
        List<String> matriz = new ArrayList<>();
        
        // Agregar encabezado con números de columnas
        if (columnasEvento > 0) {
            StringBuilder encabezado = new StringBuilder("        "); // Espacio para "Fila X: "
            for (int col = 1; col <= columnasEvento; col++) {
                encabezado.append(String.format("%-3d", col));
            }
            matriz.add(encabezado.toString());
            matriz.add(""); // Línea en blanco
        }
        
        // Generar todas las filas desde 1 hasta filasEvento, todas marcadas como libres
        if (filasEvento > 0 && columnasEvento > 0) {
            LOG.info("Generando matriz completa desde dimensiones: {} filas x {} columnas, todos libres", 
                filasEvento, columnasEvento);
            
            for (int filaNum = 1; filaNum <= filasEvento; filaNum++) {
                StringBuilder linea = new StringBuilder();
                linea.append("Fila ").append(String.format("%-3s", String.valueOf(filaNum))).append(": ");
                
                // Todas las columnas marcadas como libres
                for (int col = 1; col <= columnasEvento; col++) {
                    linea.append(String.format("%-3s", "L"));
                }
                
                matriz.add(linea.toString());
            }
        }
        
        // Agregar leyenda al final
        matriz.add("");
        matriz.add("Leyenda: L = Libre, O = Ocupado, B = Bloqueado");
        
        mapa.setMatriz(matriz);
        LOG.debug("Matriz completa generada desde dimensiones: {} líneas", matriz.size());
    }
}
