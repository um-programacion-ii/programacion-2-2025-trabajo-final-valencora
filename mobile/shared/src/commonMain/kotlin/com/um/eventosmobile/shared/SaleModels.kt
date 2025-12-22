package com.um.eventosmobile.shared

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SaleRequestDto(
    val eventoId: Long,
    val asientos: List<SeatSaleDto>
)

@Serializable
data class SeatSaleDto(
    val fila: String,
    val numero: Int,
    val nombrePersona: String,
    val apellidoPersona: String
)

@Serializable
data class SaleResponseDto(
    val id: Long? = null,
    val ventaIdCatedra: Long? = null,
    val eventoId: Long? = null,
    val fechaVenta: String? = null, // ISO 8601
    val precioVenta: Double? = null,
    val resultado: String? = null, // "EXITOSA", "FALLIDA", "PENDIENTE"
    val mensaje: String? = null,
    val asientos: List<SeatSaleResponseDto> = emptyList()
)

@Serializable
data class SeatSaleResponseDto(
    val fila: String,
    val numero: Int,
    val nombrePersona: String? = null,
    val apellidoPersona: String? = null
)

