package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aggregate statistics for one finished session (prompt 020), shown in the UI and written
 * to a `summary_<stamp>.csv` sidecar. Computed at stop from the recorded samples + events.
 */
data class SessionSummaryStats(
    val durationSec: Double,
    val saccades: Int,
    val saccadeRatePerMin: Double,
    val meanSaccadeAmplitude: Double,
    val medianSaccadeAmplitude: Double,
    val meanSaccadeDurationMs: Double,
    val medianSaccadeDurationMs: Double,
    val fixations: Int,
    val fixationRatePerMin: Double,
    val meanFixationDurationMs: Double,
    val blinks: Int,
    val blinkRatePerMin: Double,
    val meanReliability: Double,
    val trackingLossSec: Double,
)

object SummaryStats {
    private val _state = MutableStateFlow<SessionSummaryStats?>(null)
    val state: StateFlow<SessionSummaryStats?> = _state.asStateFlow()

    fun update(stats: SessionSummaryStats) {
        _state.value = stats
    }

    fun clear() {
        _state.value = null
    }
}
