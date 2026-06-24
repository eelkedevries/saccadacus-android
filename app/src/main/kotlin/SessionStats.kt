package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live session summary for the UI (prompt 007). */
data class SessionSummary(
    val recording: Boolean = false,
    val sampleCount: Int = 0,
    val sensorSampleCount: Int = 0,
    val lossIntervalCount: Int = 0,
    val sensorsActive: Boolean = false,
    val markerCount: Int = 0,
)

object SessionStats {
    private val _state = MutableStateFlow(SessionSummary())
    val state: StateFlow<SessionSummary> = _state.asStateFlow()

    fun update(summary: SessionSummary) {
        _state.value = summary
    }

    fun clear() {
        _state.value = SessionSummary()
    }
}
