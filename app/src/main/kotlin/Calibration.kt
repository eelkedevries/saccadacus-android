package com.example.saccadacusandroid

import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gaze calibration (prompt 031): an affine map from the gaze signal to a normalised screen
 * point, fitted by least squares from gaze captured at known on-screen targets. The map
 * subsumes the front-camera sign/scale conventions (spec §Domain rules).
 */
data class CalibrationModel(
    val ax: Float, val bx: Float, val cx: Float,
    val ay: Float, val by: Float, val cy: Float,
) {
    /** Map a gaze (gx, gy) to a normalised (0–1) screen point. */
    fun map(gx: Float, gy: Float): Pair<Float, Float> =
        Pair(ax * gx + bx * gy + cx, ay * gx + by * gy + cy)

    fun serialize(): String = "$ax,$bx,$cx,$ay,$by,$cy"

    companion object {
        fun deserialize(s: String): CalibrationModel? {
            val p = s.split(",").mapNotNull { it.trim().toFloatOrNull() }
            return if (p.size == 6) CalibrationModel(p[0], p[1], p[2], p[3], p[4], p[5]) else null
        }
    }
}

data class CalibrationSample(val gazeX: Float, val gazeY: Float, val screenX: Float, val screenY: Float)

/** Least-squares affine fit (gaze → normalised screen) via 3×3 normal equations. */
object GazeCalibrator {
    fun fit(samples: List<CalibrationSample>): CalibrationModel? {
        if (samples.size < 3) return null
        var sxx = 0.0; var sxy = 0.0; var sx = 0.0; var syy = 0.0; var sy = 0.0; var n = 0.0
        var bx0 = 0.0; var bx1 = 0.0; var bx2 = 0.0
        var by0 = 0.0; var by1 = 0.0; var by2 = 0.0
        for (s in samples) {
            val gx = s.gazeX.toDouble(); val gy = s.gazeY.toDouble()
            sxx += gx * gx; sxy += gx * gy; sx += gx; syy += gy * gy; sy += gy; n += 1.0
            bx0 += gx * s.screenX; bx1 += gy * s.screenX; bx2 += s.screenX
            by0 += gx * s.screenY; by1 += gy * s.screenY; by2 += s.screenY
        }
        val a = arrayOf(
            doubleArrayOf(sxx, sxy, sx),
            doubleArrayOf(sxy, syy, sy),
            doubleArrayOf(sx, sy, n),
        )
        val cx = solve3(a, doubleArrayOf(bx0, bx1, bx2)) ?: return null
        val cy = solve3(a, doubleArrayOf(by0, by1, by2)) ?: return null
        return CalibrationModel(
            cx[0].toFloat(), cx[1].toFloat(), cx[2].toFloat(),
            cy[0].toFloat(), cy[1].toFloat(), cy[2].toFloat(),
        )
    }

    private fun det3(m: Array<DoubleArray>): Double =
        m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
            m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
            m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])

    private fun solve3(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val det = det3(a)
        if (abs(det) < 1e-9) return null
        val out = DoubleArray(3)
        for (col in 0..2) {
            val m = Array(3) { r -> a[r].copyOf() }
            for (r in 0..2) m[r][col] = b[r]
            out[col] = det3(m) / det
        }
        return out
    }
}

/** Binocular-averaged eye-local gaze for a frame (the calibration input), or null. */
fun binocularGaze(frame: TrackingFrameResult?): Pair<Float, Float>? {
    if (frame == null) return null
    val xs = listOfNotNull(frame.leftEye?.irisXLocal, frame.rightEye?.irisXLocal).filter { !it.isNaN() }
    val ys = listOfNotNull(frame.leftEye?.irisYLocal, frame.rightEye?.irisYLocal).filter { !it.isNaN() }
    if (xs.isEmpty() || ys.isEmpty()) return null
    return Pair(xs.average().toFloat(), ys.average().toFloat())
}

/** Holds the active calibration (persisted by [AppSettings]); null = uncalibrated. */
object CalibrationStore {
    private val _state = MutableStateFlow<CalibrationModel?>(null)
    val state: StateFlow<CalibrationModel?> = _state.asStateFlow()

    fun set(model: CalibrationModel?) {
        _state.value = model
    }

    fun clear() {
        _state.value = null
    }
}
