package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Latest face-landmark points for the on-screen debug overlay (prompt 015). Normalised
 * image coordinates (x right, y down) of the rotated-upright frame; the UI mirrors x to a
 * selfie view to match [SignConvention]. Read-only — drawing these never affects tracking.
 */
class OverlayFrame(
    /** Interleaved normalised coordinates: [x0, y0, x1, y1, ...]. */
    val landmarks: FloatArray,
    val leftIrisIndex: Int,
    val rightIrisIndex: Int,
)

/** Toggles overlay publishing so it costs nothing when the overlay is off. */
object OverlayConfig {
    @Volatile
    var enabled: Boolean = false
}

object OverlayStats {
    private val _state = MutableStateFlow<OverlayFrame?>(null)
    val state: StateFlow<OverlayFrame?> = _state.asStateFlow()

    fun update(frame: OverlayFrame?) {
        _state.value = frame
    }

    fun clear() {
        _state.value = null
    }
}
