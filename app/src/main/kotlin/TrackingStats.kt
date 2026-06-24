package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide snapshot of the camera probe so the UI (and notification) can show
 * progress without pulling the CSV off the device. All times are
 * `SystemClock.elapsedRealtimeNanos()`.
 */
data class TrackingSnapshot(
    val active: Boolean = false,
    val frameCount: Long = 0L,
    val startElapsedRealtimeNanos: Long = 0L,
    val lastFrameElapsedRealtimeNanos: Long = 0L,
)

object TrackingStats {
    private val _state = MutableStateFlow(TrackingSnapshot())
    val state: StateFlow<TrackingSnapshot> = _state.asStateFlow()

    fun onStart(nowNanos: Long) {
        _state.value = TrackingSnapshot(
            active = true,
            frameCount = 0L,
            startElapsedRealtimeNanos = nowNanos,
            lastFrameElapsedRealtimeNanos = nowNanos,
        )
    }

    fun onFrame(nowNanos: Long) {
        val current = _state.value
        _state.value = current.copy(
            frameCount = current.frameCount + 1L,
            lastFrameElapsedRealtimeNanos = nowNanos,
        )
    }

    fun onStop() {
        _state.value = _state.value.copy(active = false)
    }
}
