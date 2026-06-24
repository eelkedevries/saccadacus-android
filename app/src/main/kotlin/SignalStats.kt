package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest derived per-frame signals for the UI (prompt 005). */
object SignalStats {
    private val _state = MutableStateFlow<TrackingFrameResult?>(null)
    val state: StateFlow<TrackingFrameResult?> = _state.asStateFlow()

    fun update(result: TrackingFrameResult) {
        _state.value = result
    }

    fun clear() {
        _state.value = null
    }
}
