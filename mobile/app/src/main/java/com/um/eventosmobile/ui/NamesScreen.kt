package com.um.eventosmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.um.eventosmobile.shared.MobileApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesScreen(
    api: MobileApi,
    eventId: Long,
    seats: List<Pair<String, Int>>,
    expiresAt: String?,
    onBack: () -> Unit,
    onExpired: () -> Unit,
    onConfirm: (List<Triple<String, Int, Pair<String, String>>>) -> Unit
) {
    // personNames se reinicializa cada vez que cambian los seats
    var personNames by remember(seats) {
        mutableStateOf(
            seats.associate { it to ("" to "") }.toMutableMap()
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Verificar expiración y mostrar contador
    var remainingSeconds by remember(expiresAt) {
        mutableStateOf<Long?>(
            expiresAt?.let { isoRaw ->
                try {
                    // Normalizar formato de fecha (puede tener microsegundos)
                    val iso = isoRaw.replace(Regex("\\.\\d+Z$"), "Z")
                    val expiryInstant = kotlinx.datetime.Instant.parse(iso)
                    val now = kotlinx.datetime.Clock.System.now()
                    val diff = expiryInstant - now
                    diff.inWholeSeconds.coerceAtLeast(0)
                } catch (e: Exception) {
                    null
                }
            }
        )
    }

    LaunchedEffect(expiresAt) {
        val start = remainingSeconds ?: return@LaunchedEffect
        if (start <= 0) {
            onExpired()
            return@LaunchedEffect
        }
        var s = start
        while (s > 0) {
            kotlinx.coroutines.delay(1_000)
            s--
            remainingSeconds = s
            if (s <= 0) {
                onExpired()
                break
            }
        }
    }

    fun validateAndContinue() {
        val invalidSeats = personNames.filter { (_, names) ->
            names.first.isBlank() || names.second.isBlank()
        }
        
        if (invalidSeats.isNotEmpty()) {
            error = "Debe completar nombre y apellido para todos los asientos"
            return
        }

        val seatsWithPeople = personNames.map { (seat, names) ->
            Triple(seat.first, seat.second, names)
        }
        onConfirm(seatsWithPeople)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos de los Pasajeros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Complete los datos de cada pasajero",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(20.dp)
            )

            // Mostrar tiempo restante si está disponible
            remainingSeconds?.let { seconds ->
                if (seconds > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Tiempo restante: ${seconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            error?.let {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            seats.forEach { (fila, numero) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Asiento: Fila $fila, Número $numero",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val seatKey = fila to numero
                        val currentNames = personNames[seatKey] ?: ("" to "")
                        
                        OutlinedTextField(
                            value = currentNames.first,
                            onValueChange = { nombre ->
                                personNames = personNames.toMutableMap().apply {
                                    put(seatKey, nombre to currentNames.second)
                                }
                                error = null
                            },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = currentNames.second,
                            onValueChange = { apellido ->
                                personNames = personNames.toMutableMap().apply {
                                    put(seatKey, currentNames.first to apellido)
                                }
                                error = null
                            },
                            label = { Text("Apellido") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { validateAndContinue() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                enabled = remainingSeconds == null || remainingSeconds!! > 0,
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    "Continuar",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

