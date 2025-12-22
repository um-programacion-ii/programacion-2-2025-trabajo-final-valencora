package com.um.eventosmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.um.eventosmobile.shared.EventDetail
import com.um.eventosmobile.shared.MobileApi
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    api: MobileApi,
    eventId: Long,
    onBack: () -> Unit,
    onViewSeats: (Long) -> Unit,
    onResumeSelection: (Long, List<Pair<String, Int>>, String?) -> Unit
) {
    var event by remember { mutableStateOf<EventDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(eventId) {
        scope.launch {
            try {
                isLoading = true
                error = null
                event = api.getEventDetail(eventId)
                
                // Verificar si hay una selección en curso
                val currentSelection = api.getCurrentSelection(eventId)
                if (currentSelection != null && currentSelection.asientos.isNotEmpty()) {
                    // Hay una selección en curso, navegar directamente a la pantalla de nombres
                    val seats = currentSelection.asientos.map { 
                        it.fila to it.numero 
                    }
                    val expiresAt = currentSelection.expiracion?.toString()
                    onResumeSelection(eventId, seats, expiresAt)
                    return@launch
                }
                
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                error = when {
                    e.message?.contains("401") == true -> "Sesión expirada"
                    e.message?.contains("404") == true -> "Evento no encontrado"
                    else -> e.message ?: "Error al cargar evento"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
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
                error != null -> {
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
                        Button(onClick = onBack) {
                            Text("Volver")
                        }
                    }
                }
                event != null -> {
                    EventDetailContent(
                        event = event!!,
                        context = context,
                        onViewSeats = { onViewSeats(eventId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EventDetailContent(
    event: EventDetail,
    context: android.content.Context,
    onViewSeats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Imagen del evento
        event.imagenUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen del evento",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = event.titulo,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tipo de evento
            event.tipoNombre?.let { tipo ->
                Text(
                    text = tipo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fecha y hora
            val localDateTime = event.fecha.toLocalDateTime(TimeZone.currentSystemDefault())
            Text(
                text = "${localDateTime.date.dayOfMonth}/${localDateTime.date.monthNumber}/${localDateTime.date.year} " +
                        "${localDateTime.time.hour.toString().padStart(2, '0')}:${localDateTime.time.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dirección
            event.direccion?.let { direccion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = direccion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Precio
            event.precio?.let { precio ->
                Text(
                    text = "Precio: $${String.format("%.2f", precio)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Descripción
            event.descripcion?.let { descripcion ->
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Resumen
            event.resumen?.let { resumen ->
                Text(
                    text = resumen,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dimensiones del mapa de asientos
            if (event.filaAsientos != null && event.columnAsientos != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Mapa de Asientos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Filas: ${event.filaAsientos}, Columnas: ${event.columnAsientos}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Botón para ver/seleccionar asientos
            Button(
                onClick = onViewSeats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Asientos y Comprar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

