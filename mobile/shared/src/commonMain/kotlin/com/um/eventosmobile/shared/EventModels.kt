package com.um.eventosmobile.shared

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// Modelos para listado de eventos
@Serializable
data class EventSummaryDto(
    val id: Long,
    val titulo: String,
    val resumen: String? = null,
    val fecha: String, // ISO 8601
    val direccion: String? = null,
    val precio: Double? = null,
    val cancelado: Boolean? = false,
    val tipoNombre: String? = null,
    val tipoDescripcion: String? = null
)

@Serializable
data class EventSummary(
    val id: Long,
    val titulo: String,
    val resumen: String?,
    val fecha: Instant,
    val direccion: String?,
    val precio: Double?,
    val cancelado: Boolean,
    val tipoNombre: String? = null,
    val tipoDescripcion: String? = null
)

// Modelos para detalle de evento
@Serializable
data class EventDetailDto(
    val id: Long,
    val eventoIdCatedra: Long? = null,
    val titulo: String,
    val descripcion: String? = null,
    val resumen: String? = null,
    val fecha: String, // ISO 8601
    val direccion: String? = null,
    val imagenUrl: String? = null,
    val precio: Double? = null,
    val cancelado: Boolean? = false,
    val tipoNombre: String? = null,
    val tipoDescripcion: String? = null,
    val filaAsientos: Int? = null,
    val columnAsientos: Int? = null,
    val integrantes: List<IntegranteDto> = emptyList(),
    val asientos: List<AsientoBloqueadoDto>? = null
)

@Serializable
data class IntegranteDto(
    val nombre: String? = null,
    val descripcion: String? = null,
    val imagenUrl: String? = null
)

@Serializable
data class AsientoBloqueadoDto(
    val fila: Int,
    val columna: Int,
    val estado: String // "LIBRE", "OCUPADO", "BLOQUEADO"
)

@Serializable
data class EventDetail(
    val id: Long,
    val eventoIdCatedra: Long?,
    val titulo: String,
    val descripcion: String?,
    val resumen: String?,
    val fecha: Instant,
    val direccion: String?,
    val imagenUrl: String?,
    val precio: Double?,
    val cancelado: Boolean,
    val tipoNombre: String?,
    val tipoDescripcion: String?,
    val filaAsientos: Int?,
    val columnAsientos: Int?,
    val integrantes: List<Integrante>,
    val asientos: List<AsientoBloqueado>?
)

@Serializable
data class Integrante(
    val nombre: String?,
    val descripcion: String?,
    val imagenUrl: String?
)

@Serializable
data class AsientoBloqueado(
    val fila: Int,
    val columna: Int,
    val estado: SeatStatus
)

enum class SeatStatus {
    LIBRE,
    OCUPADO,
    BLOQUEADO
}

