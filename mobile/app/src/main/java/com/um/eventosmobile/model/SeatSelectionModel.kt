package com.um.eventosmobile.model

import com.um.eventosmobile.shared.EventDetail
import com.um.eventosmobile.shared.SeatMap

data class SeatSelectionUiState(
    val seatMap: SeatMap? = null,
    val eventDetail: EventDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBlocking: Boolean = false,
    val selectedSeats: Set<Pair<String, Int>> = emptySet(),
    val expiresAt: String? = null
)

sealed class SeatSelectionEffect {
    data class ContinueSelection(
        val eventId: Long,
        val seats: List<Pair<String, Int>>,
        val expiresAt: String?
    ) : SeatSelectionEffect()
}

