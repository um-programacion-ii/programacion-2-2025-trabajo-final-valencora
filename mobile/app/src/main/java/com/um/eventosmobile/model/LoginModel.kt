package com.um.eventosmobile.model

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LoginEffect {
    data class LoginSuccess(val token: String) : LoginEffect()
}

