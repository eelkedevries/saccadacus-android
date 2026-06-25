package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide snapshot of the camera probe + face tracking so the UI (and notification)
 * can show progress without pulling files off the device. Frame times are
 * `SystemClock.elapsedRealtimeNanos()`.
 */
data class TrackingSnapshot(
    val active: Boolean = false,
    val paused: Boolean = false,
    val cameraLost: Boolean = false,
    val resourceWarning: String = "",
    val frameCount: Long = 0L,
    val startElapsedRealtimeNanos: Long = 0L,
    val lastFrameElapsedRealtimeNanos: Long = 0L,
    // Face landmarker (prompt 003)
    val faceDetected: Boolean = false,
    val landmarkCount: Int = 0,
    val blinkLeft: Float = 0f,
    val blinkRight: Float = 0f,
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

    fun onFace(detected: Boolean, landmarkCount: Int, blinkLeft: Float, blinkRight: Float) {
        _state.value = _state.value.copy(
            faceDetected = detected,
            landmarkCount = landmarkCount,
            blinkLeft = blinkLeft,
            blinkRight = blinkRight,
        )
    }

    fun onCameraLost(lost: Boolean) {
        if (_state.value.cameraLost != lost) {
            _state.value = _state.value.copy(cameraLost = lost)
        }
    }

    fun onResourceWarning(warning: String) {
        if (_state.value.resourceWarning != warning) {
            _state.value = _state.value.copy(resourceWarning = warning)
        }
    }

    fun onPause() {
        _state.value = _state.value.copy(paused = true)
    }

    fun onResume() {
        _state.value = _state.value.copy(paused = false)
    }

    fun onStop() {
        // resourceWarning is deliberately kept so an auto-stop reason stays visible until the
        // next start (onStart builds a fresh snapshot, clearing it).
        _state.value = _state.value.copy(active = false, paused = false, cameraLost = false, faceDetected = false)
    }
}
