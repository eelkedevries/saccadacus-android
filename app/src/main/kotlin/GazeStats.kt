package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Calibrated point-of-gaze (normalised screen, 0–1) for the UI/overlay (prompt 032).
 * Null when there is no calibration or no usable gaze this frame.
 */
object GazeStats {
    private val _state = MutableStateFlow<Pair<Float, Float>?>(null)
    val state: StateFlow<Pair<Float, Float>?> = _state.asStateFlow()

    fun update(point: Pair<Float, Float>?) {
        _state.value = point
    }

    fun clear() {
        _state.value = null
    }
}
