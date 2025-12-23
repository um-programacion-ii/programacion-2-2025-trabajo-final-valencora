package com.um.eventosmobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NamesUiState(
    val personNames: Map<Pair<String, Int>, Pair<String, String>> = emptyMap(),
    val remainingSeconds: Long? = null,
    val error: String? = null
)

sealed class NamesEffect {
    data class Expired(val eventId: Long) : NamesEffect()
    data class Confirm(
        val seatsWithPeople: List<Triple<String, Int, Pair<String, String>>>
    ) : NamesEffect()
}

class NamesViewModel(
    private val eventId: Long,
    private val seats: List<Pair<String, Int>>,
    private val expiresAt: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NamesUiState(
            personNames = seats.associate { it to ("" to "") },
            remainingSeconds = calculateRemainingSeconds(expiresAt)
        )
    )
    val uiState: StateFlow<NamesUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<NamesEffect>()
    val effects: SharedFlow<NamesEffect> = _effects.asSharedFlow()

    init {
        startCountdown()
    }

    fun updateName(seat: Pair<String, Int>, nombre: String) {
        val currentNames = _uiState.value.personNames[seat] ?: ("" to "")
        val updatedNames = _uiState.value.personNames.toMutableMap().apply {
            put(seat, nombre to currentNames.second)
        }
        _uiState.value = _uiState.value.copy(personNames = updatedNames, error = null)
    }

    fun updateLastName(seat: Pair<String, Int>, apellido: String) {
        val currentNames = _uiState.value.personNames[seat] ?: ("" to "")
        val updatedNames = _uiState.value.personNames.toMutableMap().apply {
            put(seat, currentNames.first to apellido)
        }
        _uiState.value = _uiState.value.copy(personNames = updatedNames, error = null)
    }

    fun validateAndContinue() {
        val invalidSeats = _uiState.value.personNames.filter { (_, names) ->
            names.first.isBlank() || names.second.isBlank()
        }

        if (invalidSeats.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Debe completar nombre y apellido para todos los asientos"
            )
            return
        }

        val seatsWithPeople = _uiState.value.personNames.map { (seat, names) ->
            Triple(seat.first, seat.second, names)
        }
        viewModelScope.launch {
            _effects.emit(NamesEffect.Confirm(seatsWithPeople))
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun startCountdown() {
        val initialSeconds = _uiState.value.remainingSeconds ?: return

        if (initialSeconds <= 0) {
            viewModelScope.launch {
                _effects.emit(NamesEffect.Expired(eventId))
            }
            return
        }

        viewModelScope.launch {
            var seconds = initialSeconds
            while (seconds > 0) {
                delay(1_000)
                seconds--
                _uiState.emit(_uiState.value.copy(remainingSeconds = seconds))
                if (seconds <= 0) {
                    _effects.emit(NamesEffect.Expired(eventId))
                    break
                }
            }
        }
    }

    private fun calculateRemainingSeconds(expiresAt: String?): Long? {
        return expiresAt?.let { isoRaw ->
            try {
                val iso = isoRaw.replace(Regex("\\.\\d+Z$"), "Z")
                val expiryInstant = kotlinx.datetime.Instant.parse(iso)
                val now = kotlinx.datetime.Clock.System.now()
                val diff = expiryInstant - now
                diff.inWholeSeconds.coerceAtLeast(0)
            } catch (e: Exception) {
                null
            }
        }
    }
}

class NamesViewModelFactory(
    private val eventId: Long,
    private val seats: List<Pair<String, Int>>,
    private val expiresAt: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NamesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NamesViewModel(eventId, seats, expiresAt) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

