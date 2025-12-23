package com.um.eventosmobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.shared.EventSummary
import com.um.eventosmobile.shared.MobileApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventListUiState(
    val events: List<EventSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class EventListViewModel(
    private val api: MobileApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState(isLoading = true))
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isLoading = true, error = null))
                val events = api.getEvents()
                _uiState.emit(
                    _uiState.value.copy(
                        events = events,
                        isLoading = false,
                        error = null
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(
                    _uiState.value.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("401") == true -> "Sesión expirada. Por favor inicie sesión nuevamente."
                            e.message?.contains("403") == true -> "No tiene permisos para ver eventos."
                            e.message?.contains("Network") == true -> "Error de conexión. Verifique su internet."
                            else -> e.message ?: "Error al cargar eventos"
                        }
                    )
                )
            }
        }
    }
}

class EventListViewModelFactory(
    private val api: MobileApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventListViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

