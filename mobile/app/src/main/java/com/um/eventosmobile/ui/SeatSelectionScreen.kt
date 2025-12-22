package com.um.eventosmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.shared.Seat
import com.um.eventosmobile.shared.SeatStatus
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    api: MobileApi,
    eventId: Long,
    refreshKey: Int = 0,
    onBack: () -> Unit,
    onContinue: (Long, List<Pair<String, Int>>, String?) -> Unit
) {
    var seatMap by remember { mutableStateOf<com.um.eventosmobile.shared.SeatMap?>(null) }
    var eventDetail by remember { mutableStateOf<com.um.eventosmobile.shared.EventDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isBlocking by remember { mutableStateOf(false) }
    var selectedSeats by remember { mutableStateOf<Set<Pair<String, Int>>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // Función para construir matriz completa de asientos
    fun buildCompleteSeatMap(
        map: com.um.eventosmobile.shared.SeatMap,
        filas: Int?,
        columnas: Int?
    ): com.um.eventosmobile.shared.SeatMap {
        val totalEsperado = (filas ?: 0) * (columnas ?: 0)
        
        // Si el backend ya devolvió todos los asientos (con dimensiones), usarlos directamente
        if (totalEsperado > 0 && map.asientos.size >= totalEsperado) {
            android.util.Log.d("SeatSelection", "✅ Backend ya devolvió todos los asientos: ${map.asientos.size} (esperado: $totalEsperado)")
            val estados = map.asientos.groupBy { it.estado }.mapValues { it.value.size }
            android.util.Log.d("SeatSelection", "Estados recibidos: $estados")
            // Verificar que los estados sean correctos
            val muestra = map.asientos.take(5).map { "${it.fila}-${it.numero}:${it.estado}" }
            android.util.Log.d("SeatSelection", "Muestra de asientos: $muestra")
            return map
        }
        
        if (filas == null || columnas == null) {
            android.util.Log.d("SeatSelection", "⚠️ Sin dimensiones, retornando mapa original con ${map.asientos.size} asientos")
            return map
        }
        
        // Crear mapa de asientos existentes por clave (fila-numero)
        // El proxy devuelve filas como números ("1", "2", "3")
        val asientosByKey = map.asientos.associateBy { "${it.fila}-${it.numero}" }
        
        android.util.Log.d("SeatSelection", "🔧 Construyendo matriz completa")
        android.util.Log.d("SeatSelection", "Asientos del backend: ${map.asientos.size}, dimensiones: $filas x $columnas (total esperado: $totalEsperado)")
        val estadosBackend = map.asientos.groupBy { it.estado }.mapValues { it.value.size }
        android.util.Log.d("SeatSelection", "Estados del backend: $estadosBackend")
        android.util.Log.d("SeatSelection", "Primeros asientos: ${map.asientos.take(5).map { "${it.fila}-${it.numero}:${it.estado}" }}")
        
        // El proxy usa números para filas, así que usamos números también
        // Construir lista completa de asientos
        val allSeats = mutableListOf<com.um.eventosmobile.shared.Seat>()
        var asientosConEstado = 0
        var asientosLibres = 0
        
        for (filaNum in 1..filas) {
            val filaLabel = filaNum.toString() // Usar número como el proxy
            for (columna in 1..columnas) {
                val key = "$filaLabel-$columna"
                val existingSeat = asientosByKey[key]
                
                if (existingSeat != null) {
                    // Usar el asiento del backend con su estado real
                    allSeats.add(existingSeat)
                    if (existingSeat.estado != com.um.eventosmobile.shared.SeatStatus.LIBRE) {
                        asientosConEstado++
                    } else {
                        asientosLibres++
                    }
                } else {
                    // Crear asiento libre si no existe en el backend
                    allSeats.add(
                        com.um.eventosmobile.shared.Seat(
                            fila = filaLabel,
                            numero = columna,
                            estado = com.um.eventosmobile.shared.SeatStatus.LIBRE,
                            seleccionado = false
                        )
                    )
                    asientosLibres++
                }
            }
        }
        
        android.util.Log.d("SeatSelection", "✅ Matriz completa generada: ${allSeats.size} asientos")
        val estadosFinales = allSeats.groupBy { it.estado }.mapValues { it.value.size }
        android.util.Log.d("SeatSelection", "Estados finales: $estadosFinales")
        android.util.Log.d("SeatSelection", "Asientos con estado del backend: $asientosConEstado, libres generados: $asientosLibres")
        
        return com.um.eventosmobile.shared.SeatMap(
            eventoId = map.eventoId,
            asientos = allSeats
        )
    }

    fun loadSeatMap() {
        scope.launch {
            try {
                isLoading = true
                error = null
                
                // Primero obtener el detalle del evento para tener las dimensiones
                val event = api.getEventDetail(eventId)
                eventDetail = event
                
                // Luego obtener el mapa de asientos
                val map = api.getSeatMap(eventId)
                android.util.Log.d("SeatSelection", "Mapa cargado: eventoId=${map.eventoId}, asientos=${map.asientos.size}")
                
                // Construir matriz completa de asientos
                val completeMap = buildCompleteSeatMap(map, event.filaAsientos, event.columnAsientos)
                seatMap = completeMap
                
                if (completeMap.asientos.isEmpty()) {
                    error = "No hay asientos disponibles para este evento"
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                android.util.Log.e("SeatSelection", "Error al cargar mapa: ${e.message}", e)
                error = when {
                    e.message?.contains("401") == true -> "Sesión expirada"
                    e.message?.contains("404") == true -> "Evento no encontrado"
                    e.message?.contains("500") == true -> "Error del servidor"
                    else -> e.message ?: "Error al cargar mapa de asientos"
                }
            }
        }
    }

    // Cargar mapa de asientos
    LaunchedEffect(eventId, refreshKey) {
        loadSeatMap()
    }

    fun toggleSeat(fila: String, numero: Int) {
        val seatKey = fila to numero
        selectedSeats = if (selectedSeats.contains(seatKey)) {
            selectedSeats - seatKey
        } else {
            if (selectedSeats.size < 4) {
                selectedSeats + seatKey
            } else {
                selectedSeats // No permitir más de 4
            }
        }
    }

    fun blockAndContinue() {
        if (selectedSeats.isEmpty()) {
            error = "Debe seleccionar al menos un asiento"
            return
        }
        if (selectedSeats.size > 4) {
            error = "Puede seleccionar máximo 4 asientos"
            return
        }

        scope.launch {
            try {
                isBlocking = true
                error = null
                
                // Primero actualizar el evento seleccionado en la sesión
                api.updateSelectedEvent(eventId)
                
                // Luego guardar los asientos seleccionados en la sesión
                val asientosDto = selectedSeats.map { (fila, numero) ->
                    com.um.eventosmobile.shared.AsientoSeleccionadoDto(
                        fila = fila,
                        numero = numero,
                        nombrePersona = null,
                        apellidoPersona = null
                    )
                }
                api.updateSelectedSeats(asientosDto)
                
                // Ahora bloquear los asientos (el backend los obtiene de la sesión)
                val blockResponse = api.blockSeats(eventId)
                
                if (blockResponse.exitoso == true) {
                    // Obtener la expiración de la selección actual
                    val selection = api.getCurrentSelection(eventId)
                    val expiresAt = selection?.expiracion?.toString()
                    
                    isBlocking = false
                    onContinue(eventId, selectedSeats.toList(), expiresAt)
                } else {
                    error = blockResponse.mensaje ?: "Error al bloquear asientos"
                    isBlocking = false
                }
            } catch (e: Exception) {
                isBlocking = false
                error = e.message ?: "Error al bloquear asientos"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Asientos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Asientos seleccionados: ${selectedSeats.size}/4",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { blockAndContinue() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedSeats.isNotEmpty() && !isBlocking
                    ) {
                        if (isBlocking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Continuar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && seatMap == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadSeatMap() }) {
                            Text("Reintentar")
                        }
                    }
                }
                seatMap != null -> {
                    if (seatMap!!.asientos.isEmpty()) {
                        // Lista vacía
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No hay asientos disponibles",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadSeatMap() }) {
                                Text("Reintentar")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Leyenda
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    Color(0xFF4CAF50),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Libre", style = MaterialTheme.typography.bodySmall)
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    Color(0xFFF44336),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ocupado", style = MaterialTheme.typography.bodySmall)
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    Color(0xFFFF9800),
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Bloqueado", style = MaterialTheme.typography.bodySmall)
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Seleccionado", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Error si hay
                            error?.let {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                        // Mapa de asientos - agrupar por fila y mostrar en grid
                        val seatsByRow = seatMap!!.asientos.groupBy { it.fila }
                        val columnas = eventDetail?.columnAsientos ?: seatsByRow.values.maxOfOrNull { it.size } ?: 10
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnas),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            seatsByRow.forEach { (fila, seats) ->
                                item(span = { GridItemSpan(columnas) }) {
                                    Text(
                                        text = "Fila $fila",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(seats.sortedBy { it.numero }) { seat ->
                                    SeatItem(
                                        seat = seat,
                                        isSelected = selectedSeats.contains(seat.fila to seat.numero),
                                        onClick = {
                                            if (seat.estado == SeatStatus.LIBRE) {
                                                toggleSeat(seat.fila, seat.numero)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatItem(
    seat: Seat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (seat.estado) {
        SeatStatus.LIBRE -> Color(0xFF4CAF50) // Verde
        SeatStatus.OCUPADO -> Color(0xFFF44336) // Rojo
        SeatStatus.BLOQUEADO -> Color(0xFFFF9800) // Naranja
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val borderWidth = if (isSelected) 3.dp else 0.dp
    
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                enabled = seat.estado == SeatStatus.LIBRE,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seat.numero.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (seat.estado == SeatStatus.LIBRE) Color.White else Color.White
        )
    }
}

