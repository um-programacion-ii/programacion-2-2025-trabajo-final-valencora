package com.um.eventosmobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.shared.SaleRequestDto
import com.um.eventosmobile.shared.SaleResponseDto
import com.um.eventosmobile.shared.SeatSaleDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaleConfirmationUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class SaleConfirmationEffect {
    data class SaleCompleted(val response: SaleResponseDto) : SaleConfirmationEffect()
}

class SaleConfirmationViewModel(
    private val api: MobileApi,
    private val eventId: Long,
    private val seatsWithPeople: List<Triple<String, Int, Pair<String, String>>>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleConfirmationUiState())
    val uiState: StateFlow<SaleConfirmationUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SaleConfirmationEffect>()
    val effects: SharedFlow<SaleConfirmationEffect> = _effects.asSharedFlow()

    fun processSale() {
        viewModelScope.launch {
            try {
                _uiState.emit(_uiState.value.copy(isLoading = true, error = null))

                val saleRequest = SaleRequestDto(
                    eventoId = eventId,
                    asientos = seatsWithPeople.map { (fila, numero, names) ->
                        SeatSaleDto(
                            fila = fila,
                            numero = numero,
                            nombrePersona = names.first,
                            apellidoPersona = names.second
                        )
                    }
                )

                val response = api.processSale(saleRequest)
                _uiState.emit(_uiState.value.copy(isLoading = false))
                _effects.emit(SaleConfirmationEffect.SaleCompleted(response))
            } catch (e: Exception) {
                _uiState.emit(
                    _uiState.value.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("401") == true -> "Sesión expirada"
                            e.message?.contains("400") == true -> "Datos inválidos"
                            else -> e.message ?: "Error al procesar la venta"
                        }
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class SaleConfirmationViewModelFactory(
    private val api: MobileApi,
    private val eventId: Long,
    private val seatsWithPeople: List<Triple<String, Int, Pair<String, String>>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleConfirmationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaleConfirmationViewModel(api, eventId, seatsWithPeople) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

