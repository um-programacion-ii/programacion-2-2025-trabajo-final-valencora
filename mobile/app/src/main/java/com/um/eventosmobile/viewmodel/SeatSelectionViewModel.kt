package com.um.eventosmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.model.SeatSelectionEffect
import com.um.eventosmobile.model.SeatSelectionUiState
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.shared.Seat
import com.um.eventosmobile.shared.SeatMap
import com.um.eventosmobile.shared.SeatStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeatSelectionViewModel(
    private val api: MobileApi,
    private val eventId: Long,
    private val refreshKey: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeatSelectionUiState(isLoading = true))
    val uiState: StateFlow<SeatSelectionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SeatSelectionEffect>()
    val effects: SharedFlow<SeatSelectionEffect> = _effects.asSharedFlow()

    init {
        loadSeatMap()
    }

    fun loadSeatMap() {
        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isLoading = true, error = null))

                val event = api.getEventDetail(eventId)
                val map = api.getSeatMap(eventId)

                val completeMap = buildCompleteSeatMap(map, event.filaAsientos, event.columnAsientos)

                // Verificar si hay una selección activa para este evento
                val currentSelection = try {
                    api.getCurrentSelection(eventId)
                } catch (e: Exception) {
                    null
                }
                
                // Si hay una selección activa, restaurarla
                val activeSeats = currentSelection?.asientos?.mapNotNull { seat ->
                    seat.numero?.let { seat.fila to it }
                }?.toSet() ?: emptySet()
                val expiresAt = currentSelection?.expiracion?.toString()

                _uiState.emit(
                    _uiState.value.copy(
                        seatMap = completeMap,
                        eventDetail = event,
                        isLoading = false,
                        selectedSeats = activeSeats,
                        expiresAt = expiresAt,
                        error = if (completeMap.asientos.isEmpty()) {
                            "No hay asientos disponibles para este evento"
                        } else null
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(
                    _uiState.value.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("401") == true -> "Sesión expirada"
                            e.message?.contains("404") == true -> "Evento no encontrado"
                            e.message?.contains("500") == true -> "Error del servidor"
                            else -> e.message ?: "Error al cargar mapa de asientos"
                        }
                    )
                )
            }
        }
    }

    fun toggleSeat(fila: String, numero: Int) {
        val seatKey = fila to numero
        val currentSelected = _uiState.value.selectedSeats
        val newSelected = if (currentSelected.contains(seatKey)) {
            currentSelected - seatKey
        } else {
            if (currentSelected.size < 4) {
                currentSelected + seatKey
            } else {
                currentSelected
            }
        }
        _uiState.value = _uiState.value.copy(selectedSeats = newSelected)
    }

    fun blockAndContinue() {
        val selectedSeats = _uiState.value.selectedSeats
        if (selectedSeats.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Debe seleccionar al menos un asiento")
            return
        }
        if (selectedSeats.size > 4) {
            _uiState.value = _uiState.value.copy(error = "Puede seleccionar máximo 4 asientos")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isBlocking = true, error = null))

                api.updateSelectedEvent(eventId)

                val asientosDto = selectedSeats.map { (fila, numero) ->
                    com.um.eventosmobile.shared.AsientoSeleccionadoDto(
                        fila = fila,
                        numero = numero,
                        nombrePersona = null,
                        apellidoPersona = null
                    )
                }
                api.updateSelectedSeats(asientosDto)

                val blockResponse = api.blockSeats(eventId)

                if (blockResponse.exitoso == true) {
                    val selection = api.getCurrentSelection(eventId)
                    val expiresAt = selection?.expiracion?.toString()

                    _uiState.emit(_uiState.value.copy(
                        isBlocking = false,
                        error = null,
                        selectedSeats = selectedSeats,
                        expiresAt = expiresAt
                    ))
                    _effects.emit(
                        SeatSelectionEffect.ContinueSelection(
                            eventId = eventId,
                            seats = selectedSeats.toList(),
                            expiresAt = expiresAt
                        )
                    )
                } else {
                    _uiState.emit(
                        _uiState.value.copy(
                            isBlocking = false,
                            error = blockResponse.mensaje ?: "Error al bloquear asientos"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.emit(
                    _uiState.value.copy(
                        isBlocking = false,
                        error = e.message ?: "Error al bloquear asientos"
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun buildCompleteSeatMap(
        map: SeatMap,
        filas: Int?,
        columnas: Int?
    ): SeatMap {
        val totalEsperado = (filas ?: 0) * (columnas ?: 0)

        if (totalEsperado > 0 && map.asientos.size >= totalEsperado) {
            return map
        }

        if (filas == null || columnas == null) {
            return map
        }

        val asientosByKey = map.asientos.associateBy { "${it.fila}-${it.numero}" }
        val allSeats = mutableListOf<Seat>()

        for (filaNum in 1..filas) {
            val filaLabel = filaNum.toString()
            for (columna in 1..columnas) {
                val key = "$filaLabel-$columna"
                val existingSeat = asientosByKey[key]

                if (existingSeat != null) {
                    allSeats.add(existingSeat)
                } else {
                    allSeats.add(
                        Seat(
                            fila = filaLabel,
                            numero = columna,
                            estado = SeatStatus.LIBRE,
                            seleccionado = false
                        )
                    )
                }
            }
        }

        return SeatMap(
            eventoId = map.eventoId,
            asientos = allSeats
        )
    }
}

class SeatSelectionViewModelFactory(
    private val api: MobileApi,
    private val eventId: Long,
    private val refreshKey: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeatSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeatSelectionViewModel(api, eventId, refreshKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

