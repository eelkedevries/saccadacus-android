package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EventCounts(
    val saccades: Long = 0L,
    val blinks: Long = 0L,
    val fixations: Long = 0L,
    val headMotionLabel: String = "-",
)

object EventStats {
    private val _state = MutableStateFlow(EventCounts())
    val state: StateFlow<EventCounts> = _state.asStateFlow()

    fun update(counts: EventCounts) {
        _state.value = counts
    }

    fun clear() {
        _state.value = EventCounts()
    }
}
