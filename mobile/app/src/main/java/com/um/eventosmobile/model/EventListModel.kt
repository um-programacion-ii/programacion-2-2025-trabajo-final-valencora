package com.um.eventosmobile.model

import com.um.eventosmobile.shared.EventSummary

data class EventListUiState(
    val events: List<EventSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

