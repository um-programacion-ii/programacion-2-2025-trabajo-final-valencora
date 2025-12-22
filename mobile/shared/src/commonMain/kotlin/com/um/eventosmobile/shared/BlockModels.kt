package com.um.eventosmobile.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlockSeatsResponseDto(
    @SerialName("exitoso")
    val exitoso: Boolean? = null,
    @SerialName("mensaje")
    val mensaje: String? = null,
    val eventoId: Long? = null,
    val asientosBloqueados: List<AsientoBloqueoDto> = emptyList(),
    val asientosNoDisponibles: List<AsientoBloqueoDto> = emptyList()
) {
    // Propiedades de compatibilidad para facilitar el uso
    val resultado: Boolean
        get() = exitoso ?: false
    
    val descripcion: String?
        get() = mensaje
}

@Serializable
data class AsientoBloqueoDto(
    val fila: String? = null,  // El backend puede devolver String o null
    val numero: Int? = null     // El backend puede devolver null
) {
    // Propiedad de compatibilidad
    val columna: Int?
        get() = numero
}

