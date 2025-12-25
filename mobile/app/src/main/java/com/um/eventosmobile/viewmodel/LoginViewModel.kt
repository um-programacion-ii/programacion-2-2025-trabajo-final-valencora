package com.um.eventosmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.model.LoginEffect
import com.um.eventosmobile.model.LoginUiState
import com.um.eventosmobile.shared.AuthApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val currentState = _uiState.value

        // Validación
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "Por favor complete todos los campos")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.emit(currentState.copy(isLoading = true, error = null))

                val token = authApi.login(
                    currentState.username.trim(),
                    currentState.password.trim()
                )

                _uiState.emit(currentState.copy(isLoading = false))
                _effects.emit(LoginEffect.LoginSuccess(token))
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Credenciales inválidas"
                    e.message?.contains("403") == true -> "Acceso denegado"
                    e.message?.contains("Network") == true -> "Error de conexión. Verifique su internet"
                    else -> e.message ?: "Error al iniciar sesión"
                }
                _uiState.emit(currentState.copy(isLoading = false, error = errorMessage))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class LoginViewModelFactory(
    private val authApi: AuthApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

