package com.um.eventosmobile.shared

import kotlinx.serialization.Serializable

@Serializable
data class SeatMapDto(
    val eventoId: Long,
    val asientos: List<SeatDto>
)

@Serializable
data class SeatDto(
    val fila: String,
    val numero: Int,
    val estado: String, // "LIBRE", "OCUPADO", "BLOQUEADO" - viene como string del backend
    val seleccionado: Boolean = false
)

@Serializable
data class SeatMap(
    val eventoId: Long,
    val asientos: List<Seat>
)

@Serializable
data class Seat(
    val fila: String,
    val numero: Int,
    val estado: SeatStatus,
    val seleccionado: Boolean = false
)

