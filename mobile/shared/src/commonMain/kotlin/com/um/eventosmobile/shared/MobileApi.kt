package com.um.eventosmobile.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

/**
 * Cliente API para comunicación con el backend y el proxy.
 * Agrega automáticamente el token JWT a todas las peticiones.
 * 
 * Endpoints que van al PROXY:
 * - Obtener mapa de asientos
 * - Confirmar venta
 * 
 * Endpoints que van al BACKEND:
 * - Bloquear asientos (el backend obtiene los asientos de la sesión y los envía al proxy)
 * 
 * El resto de endpoints van al BACKEND.
 */
class MobileApi(
    private val backendUrl: String,  // Backend: eventos, sesión, etc.
    private val proxyUrl: String,     // Proxy: mapa de asientos, bloquear, ventas
    private val tokenProvider: () -> String?
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }
    }
    
    // Función helper para agregar token a las peticiones
    private fun HttpRequestBuilder.addAuthToken() {
        tokenProvider()?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    /**
     * GET /api/eventos - Obtiene el listado de eventos activos (BACKEND)
     */
    suspend fun getEvents(): List<EventSummary> {
        val response: List<EventSummaryDto> = client.get("$backendUrl/api/eventos") {
            addAuthToken()
        }.body()
        
        return response.map { dto ->
            EventSummary(
                id = dto.id,
                titulo = dto.titulo,
                resumen = dto.resumen,
                fecha = Instant.parse(dto.fecha),
                direccion = dto.direccion,
                precio = dto.precio,
                cancelado = dto.cancelado ?: false,
                tipoNombre = dto.tipoNombre,
                tipoDescripcion = dto.tipoDescripcion
            )
        }
    }

    /**
     * GET /api/eventos/{id} - Obtiene el detalle de un evento (BACKEND)
     */
    suspend fun getEventDetail(eventId: Long): EventDetail {
        val dto: EventDetailDto = client.get("$backendUrl/api/eventos/$eventId") {
            addAuthToken()
        }.body()
        
        return EventDetail(
            id = dto.id,
            eventoIdCatedra = dto.eventoIdCatedra,
            titulo = dto.titulo,
            descripcion = dto.descripcion,
            resumen = dto.resumen,
            fecha = Instant.parse(dto.fecha),
            direccion = dto.direccion,
            imagenUrl = dto.imagenUrl,
            precio = dto.precio,
            cancelado = dto.cancelado ?: false,
            tipoNombre = dto.tipoNombre,
            tipoDescripcion = dto.tipoDescripcion,
            filaAsientos = dto.filaAsientos,
            columnAsientos = dto.columnAsientos,
            integrantes = dto.integrantes.map { 
                Integrante(it.nombre, it.descripcion, it.imagenUrl) 
            },
            asientos = dto.asientos?.map { 
                AsientoBloqueado(
                    fila = it.fila,
                    columna = it.columna,
                    estado = when (it.estado.uppercase()) {
                        "LIBRE" -> SeatStatus.LIBRE
                        "OCUPADO" -> SeatStatus.OCUPADO
                        "BLOQUEADO" -> SeatStatus.BLOQUEADO
                        else -> SeatStatus.LIBRE
                    }
                )
            }
        )
    }

    /**
     * GET /api/asientos/evento/{eventoId} - Obtiene el mapa de asientos de un evento (PROXY)
     */
    suspend fun getSeatMap(eventoId: Long): SeatMap {
        val dto: SeatMapDto = client.get("$proxyUrl/api/asientos/evento/$eventoId") {
            addAuthToken()
        }.body()
        
        return SeatMap(
            eventoId = dto.eventoId,
            asientos = dto.asientos.map { seatDto ->
                Seat(
                    fila = seatDto.fila,
                    numero = seatDto.numero,
                    estado = when (seatDto.estado.uppercase().trim()) {
                        "LIBRE" -> SeatStatus.LIBRE
                        "OCUPADO" -> SeatStatus.OCUPADO
                        "BLOQUEADO" -> SeatStatus.BLOQUEADO
                        else -> SeatStatus.LIBRE
                    },
                    seleccionado = seatDto.seleccionado
                )
            }
        )
    }

    /**
     * POST /api/asientos/bloquear/{eventoId} - Bloquea los asientos seleccionados (BACKEND)
     * El backend obtiene los asientos de la sesión del usuario y los envía al proxy
     */
    suspend fun blockSeats(eventoId: Long): BlockSeatsResponseDto {
        return client.post("$backendUrl/api/asientos/bloquear/$eventoId") {
            addAuthToken()
            contentType(ContentType.Application.Json)
        }.body()
    }

    /**
     * POST /api/ventas/confirmar - Confirma una venta (PROXY)
     */
    suspend fun processSale(request: SaleRequestDto): SaleResponseDto {
        return client.post("$proxyUrl/api/ventas/confirmar") {
            addAuthToken()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * GET /api/sesion/estado - Obtiene el estado actual de la sesión (BACKEND)
     */
    suspend fun getSessionState(): SessionState {
        val dto: SessionStateDto = client.get("$backendUrl/api/sesion/estado") {
            addAuthToken()
        }.body()
        
        return SessionState(
            eventoId = dto.eventoId,
            etapa = dto.etapa?.let { 
                try {
                    SessionStage.valueOf(it.uppercase())
                } catch (e: Exception) {
                    null
                }
            },
            asientos = dto.asientos.map {
                SelectedSeat(
                    fila = it.fila,
                    numero = it.numero,
                    nombrePersona = it.nombrePersona,
                    apellidoPersona = it.apellidoPersona
                )
            },
            expiracion = dto.expiracion?.let { Instant.parse(it) }
        )
    }

    /**
     * Obtiene el estado de selección actual para un evento específico.
     * Si hay una selección en curso, devuelve los asientos y la expiración.
     */
    suspend fun getCurrentSelection(eventoId: Long): SessionState? {
        val state = getSessionState()
        return if (state.eventoId == eventoId && state.asientos.isNotEmpty()) {
            state
        } else {
            null
        }
    }

    /**
     * PUT /api/sesion/estado - Guarda el estado completo de la sesión (BACKEND)
     */
    suspend fun saveSessionState(state: SessionStateDto) {
        client.put("$backendUrl/api/sesion/estado") {
            addAuthToken()
            contentType(ContentType.Application.Json)
            setBody(state)
        }
    }

    /**
     * PUT /api/sesion/evento/{eventoId} - Actualiza el evento seleccionado (BACKEND)
     */
    suspend fun updateSelectedEvent(eventoId: Long) {
        client.put("$backendUrl/api/sesion/evento/$eventoId") {
            addAuthToken()
        }
    }

    /**
     * PUT /api/sesion/asientos - Actualiza los asientos seleccionados (BACKEND)
     */
    suspend fun updateSelectedSeats(asientos: List<AsientoSeleccionadoDto>) {
        client.put("$backendUrl/api/sesion/asientos") {
            addAuthToken()
            contentType(ContentType.Application.Json)
            setBody(asientos.toTypedArray())
        }
    }

    /**
     * PUT /api/sesion/nombres - Actualiza los nombres de las personas (BACKEND)
     */
    suspend fun updateNames(nombresPorAsiento: Map<String, AsientoSeleccionadoDto>) {
        client.put("$backendUrl/api/sesion/nombres") {
            addAuthToken()
            contentType(ContentType.Application.Json)
            setBody(nombresPorAsiento)
        }
    }

    /**
     * DELETE /api/sesion/estado - Limpia el estado de la sesión (BACKEND)
     */
    suspend fun clearSessionState() {
        client.delete("$backendUrl/api/sesion/estado") {
            addAuthToken()
        }
    }
}
