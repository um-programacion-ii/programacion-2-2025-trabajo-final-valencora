package com.um.eventosmobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.model.RegisterEffect
import com.um.eventosmobile.model.RegisterUiState
import com.um.eventosmobile.shared.AuthApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authApi: AuthApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<RegisterEffect>()
    val effects: SharedFlow<RegisterEffect> = _effects.asSharedFlow()

    fun updateLogin(login: String) {
        _uiState.value = _uiState.value.copy(login = login, error = null)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun register() {
        val currentState = _uiState.value

        // Validaciones
        if (currentState.login.isBlank() || currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "Por favor complete todos los campos")
            return
        }

        if (currentState.password.length < 4) {
            _uiState.value = currentState.copy(error = "La contraseña debe tener al menos 4 caracteres")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.emit(currentState.copy(isLoading = true, error = null))

                authApi.register(
                    currentState.login.trim(),
                    currentState.email.trim(),
                    currentState.password
                )

                _uiState.emit(currentState.copy(isLoading = false, success = true))
                _effects.emit(RegisterEffect.RegisterSuccess)
            } catch (e: Exception) {
                val errorMessage = e.message ?: e.toString()
                val error = when {
                    errorMessage.contains("400") -> {
                        when {
                            errorMessage.contains("login") || errorMessage.contains("username") || errorMessage.contains("LOGIN_ALREADY_USED") ->
                                "El nombre de usuario ya está en uso"
                            errorMessage.contains("email") || errorMessage.contains("EMAIL_ALREADY_USED") ->
                                "El correo electrónico ya está en uso"
                            else -> "Datos inválidos. Verifique la información ingresada"
                        }
                    }
                    errorMessage.contains("timeout") || errorMessage.contains("Socket timeout") || errorMessage.contains("timed out") ->
                        "Error de conexión. Verifique que el backend esté corriendo y ejecute: adb reverse tcp:8080 tcp:8080"
                    errorMessage.contains("Network") || errorMessage.contains("Unable to resolve") || errorMessage.contains("failed to connect") || errorMessage.contains("Connection refused") ->
                        "Error de conexión. Verifique que el backend esté corriendo en localhost:8080 y ejecute: adb reverse tcp:8080 tcp:8080"
                    else -> "Error al registrarse: $errorMessage"
                }
                _uiState.emit(currentState.copy(isLoading = false, error = error))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }
}

class RegisterViewModelFactory(
    private val authApi: AuthApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

