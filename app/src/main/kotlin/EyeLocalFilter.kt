package com.example.saccadacusandroid

/**
 * Optional smoothing of the eye-local signal before recording and event detection
 * (prompt 021). A per-eye, per-axis exponential moving average. Strictly opt-in: when
 * [SessionConfig.filterEnabled] is off the frame is returned unchanged (and the filter
 * state is reset), so the default signal path is byte-for-byte the unfiltered behaviour.
 * Fed from a single thread (the MediaPipe result callback).
 */
class EyeLocalFilter {
    private var lx = Float.NaN
    private var ly = Float.NaN
    private var rx = Float.NaN
    private var ry = Float.NaN

    fun reset() {
        lx = Float.NaN
        ly = Float.NaN
        rx = Float.NaN
        ry = Float.NaN
    }

    fun process(frame: TrackingFrameResult): TrackingFrameResult {
        if (!SessionConfig.filterEnabled) {
            reset()
            return frame
        }
        val a = SessionConfig.filterAlpha
        val left = frame.leftEye?.let { e ->
            lx = ema(lx, e.irisXLocal, a)
            ly = ema(ly, e.irisYLocal, a)
            e.copy(irisXLocal = lx, irisYLocal = ly)
        }
        val right = frame.rightEye?.let { e ->
            rx = ema(rx, e.irisXLocal, a)
            ry = ema(ry, e.irisYLocal, a)
            e.copy(irisXLocal = rx, irisYLocal = ry)
        }
        return frame.copy(leftEye = left, rightEye = right)
    }

    private fun ema(prev: Float, x: Float, a: Float): Float = if (prev.isNaN()) x else a * x + (1f - a) * prev
}
