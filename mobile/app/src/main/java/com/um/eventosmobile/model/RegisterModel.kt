package com.um.eventosmobile.model

data class RegisterUiState(
    val login: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

sealed class RegisterEffect {
    object RegisterSuccess : RegisterEffect()
}

