package com.um.eventosmobile.shared

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SessionStateDto(
    val eventoId: Long? = null,
    val etapa: String? = null, // "SELECCION_EVENTO", "SELECCION_ASIENTOS", "CARGA_NOMBRES", "CONFIRMACION"
    val asientos: List<AsientoSeleccionadoDto> = emptyList(),
    val expiracion: String? = null // ISO 8601
)

@Serializable
data class AsientoSeleccionadoDto(
    val fila: String,
    val numero: Int,
    val nombrePersona: String? = null,
    val apellidoPersona: String? = null
)

@Serializable
data class SessionState(
    val eventoId: Long?,
    val etapa: SessionStage?,
    val asientos: List<SelectedSeat>,
    val expiracion: Instant?
)

@Serializable
data class SelectedSeat(
    val fila: String,
    val numero: Int,
    val nombrePersona: String? = null,
    val apellidoPersona: String? = null
)

enum class SessionStage {
    SELECCION_EVENTO,
    SELECCION_ASIENTOS,
    CARGA_NOMBRES,
    CONFIRMACION
}

