package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Advisory recording-quality state for the UI and notification (prompt 016): a smoothed
 * lighting/face indicator so a poor session (e.g. low light, where the iris collapses to
 * the eye centre) is flagged while it happens. Advisory only — never alters recording.
 */
data class QualitySnapshot(
    /** One of: "idle", "good", "low_light", "face_lost". */
    val label: String = "idle",
    /** Smoothed mean luma of the analysis frame, 0–255. */
    val luma: Double = 0.0,
) {
    companion object {
        const val IDLE = "idle"
        const val GOOD = "good"
        const val LOW_LIGHT = "low_light"
        const val FACE_LOST = "face_lost"
    }
}

object QualityStats {
    private val _state = MutableStateFlow(QualitySnapshot())
    val state: StateFlow<QualitySnapshot> = _state.asStateFlow()

    fun update(label: String, luma: Double) {
        _state.value = QualitySnapshot(label, luma)
    }

    fun clear() {
        _state.value = QualitySnapshot()
    }
}
