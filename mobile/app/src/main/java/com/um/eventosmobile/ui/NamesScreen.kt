package com.um.eventosmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.ui.state.NamesEffect
import com.um.eventosmobile.ui.state.NamesViewModel
import com.um.eventosmobile.ui.state.NamesViewModelFactory
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesScreen(
    api: MobileApi,
    eventId: Long,
    seats: List<Pair<String, Int>>,
    expiresAt: String?,
    onBack: () -> Unit,
    onExpired: () -> Unit,
    onConfirm: (List<Triple<String, Int, Pair<String, String>>>) -> Unit,
    viewModel: NamesViewModel = viewModel(
        factory = NamesViewModelFactory(eventId, seats, expiresAt)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is NamesEffect.Expired -> {
                    onExpired()
                }
                is NamesEffect.Confirm -> {
                    onConfirm(effect.seatsWithPeople)
                }
            }
        }
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
            uiState.remainingSeconds?.let { seconds ->
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

            uiState.error?.let {
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
                        val currentNames = uiState.personNames[seatKey] ?: ("" to "")
                        
                        OutlinedTextField(
                            value = currentNames.first,
                            onValueChange = { nombre ->
                                viewModel.updateName(seatKey, nombre)
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
                                viewModel.updateLastName(seatKey, apellido)
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
                onClick = { viewModel.validateAndContinue() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                enabled = uiState.remainingSeconds == null || uiState.remainingSeconds!! > 0,
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

