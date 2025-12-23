package com.um.eventosmobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.shared.EventDetail
import com.um.eventosmobile.shared.MobileApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: EventDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EventDetailEffect {
    data class ResumeSelection(
        val eventId: Long,
        val seats: List<Pair<String, Int>>,
        val expiresAt: String?
    ) : EventDetailEffect()
}

class EventDetailViewModel(
    private val api: MobileApi,
    private val eventId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState(isLoading = true))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EventDetailEffect>()
    val effects: SharedFlow<EventDetailEffect> = _effects.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isLoading = true, error = null))

                val event = api.getEventDetail(eventId)
                val currentSelection = api.getCurrentSelection(eventId)

                if (currentSelection != null && currentSelection.asientos.isNotEmpty()) {
                    val seats = currentSelection.asientos.map { it.fila to it.numero }
                    val expiresAt = currentSelection.expiracion?.toString()
                    _effects.emit(
                        EventDetailEffect.ResumeSelection(
                            eventId = eventId,
                            seats = seats,
                            expiresAt = expiresAt
                        )
                    )
                }

                _uiState.emit(
                    _uiState.value.copy(
                        event = event,
                        isLoading = false,
                        error = null
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(
                    _uiState.value.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("401") == true -> "Sesión expirada"
                            e.message?.contains("404") == true -> "Evento no encontrado"
                            else -> e.message ?: "Error al cargar evento"
                        }
                    )
                )
            }
        }
    }
}

class EventDetailViewModelFactory(
    private val api: MobileApi,
    private val eventId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventDetailViewModel(api, eventId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

