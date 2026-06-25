package com.example.saccadacusandroid

import kotlin.math.PI
import kotlin.math.abs

/**
 * One-Euro filter (prompt 037): a speed-adaptive low-pass for the live point-of-gaze (the 1€
 * Filter, Casiez et al., CHI 2012). At low gaze velocity (fixations) the cutoff is low so the
 * gaze dot is smoothed heavily; at high velocity (saccades) the cutoff rises so it stays
 * responsive. Time-aware, so it tolerates the variable analysis cadence. Single-threaded (fed
 * from the MediaPipe result callback).
 */
class OneEuroFilter(
    private val minCutoff: Float = MIN_CUTOFF,
    private val beta: Float = BETA,
    private val dCutoff: Float = D_CUTOFF,
) {
    private var xPrev = Float.NaN
    private var dxPrev = 0f
    private var tPrevNanos = 0L

    fun reset() {
        xPrev = Float.NaN
        dxPrev = 0f
        tPrevNanos = 0L
    }

    fun filter(x: Float, tNanos: Long): Float {
        if (xPrev.isNaN()) {
            xPrev = x
            dxPrev = 0f
            tPrevNanos = tNanos
            return x
        }
        val dt = ((tNanos - tPrevNanos).coerceAtLeast(1L)) / 1e9f
        tPrevNanos = tNanos
        val dx = (x - xPrev) / dt
        dxPrev = lowpass(dx, dxPrev, alpha(dCutoff, dt))
        val cutoff = minCutoff + beta * abs(dxPrev)
        xPrev = lowpass(x, xPrev, alpha(cutoff, dt))
        return xPrev
    }

    private fun lowpass(x: Float, prev: Float, a: Float): Float = a * x + (1f - a) * prev

    private fun alpha(cutoff: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoff)
        return 1f / (1f + tau / dt)
    }

    companion object {
        const val MIN_CUTOFF = 1.0f  // Hz: smoothing strength during fixations
        const val BETA = 1.0f        // speed coefficient: raises the cutoff during saccades
        const val D_CUTOFF = 1.0f    // Hz: low-pass on the derivative used to drive the cutoff
    }
}

/** Two One-Euro filters (x, y) for the binocular gaze that feeds the point-of-gaze (prompt 037). */
class GazeSmoother {
    private val fx = OneEuroFilter()
    private val fy = OneEuroFilter()

    fun reset() {
        fx.reset()
        fy.reset()
    }

    fun filter(gx: Float, gy: Float, tNanos: Long): Pair<Float, Float> =
        Pair(fx.filter(gx, tNanos), fy.filter(gy, tNanos))
}
