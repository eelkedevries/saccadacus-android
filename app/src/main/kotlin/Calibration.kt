package com.example.saccadacusandroid

import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gaze calibration (prompt 031; polynomial map prompt 038): maps the gaze signal to a normalised
 * screen point. The map is a **2nd-order polynomial** over the basis `[1, gx, gy, gx·gy, gx², gy²]`,
 * fitted by least squares from gaze captured at known on-screen targets, with a safe **affine
 * fallback** (higher-order coefficients zero) when there are too few points. It subsumes the
 * front-camera sign/scale conventions (spec §Domain rules).
 */
class CalibrationModel(val cx: FloatArray, val cy: FloatArray) {
    init {
        require(cx.size == N && cy.size == N) { "calibration needs $N coefficients per axis" }
    }

    /** Map a gaze (gx, gy) to a normalised (0–1) screen point. */
    fun map(gx: Float, gy: Float): Pair<Float, Float> {
        val b = basis(gx, gy)
        return Pair(dot(cx, b), dot(cy, b))
    }

    fun serialize(): String = (cx + cy).joinToString(",")

    companion object {
        const val N = 6 // basis size: [1, gx, gy, gx*gy, gx^2, gy^2]

        fun basis(gx: Float, gy: Float): FloatArray =
            floatArrayOf(1f, gx, gy, gx * gy, gx * gx, gy * gy)

        private fun dot(c: FloatArray, b: FloatArray): Float {
            var s = 0f
            for (i in c.indices) s += c[i] * b[i]
            return s
        }

        /** Embed a legacy affine map (x = a·gx + b·gy + c) into the polynomial model. */
        fun affine(ax: Float, bx: Float, cx0: Float, ay: Float, by: Float, cy0: Float): CalibrationModel =
            CalibrationModel(
                floatArrayOf(cx0, ax, bx, 0f, 0f, 0f),
                floatArrayOf(cy0, ay, by, 0f, 0f, 0f),
            )

        fun deserialize(s: String): CalibrationModel? {
            val p = s.split(",").mapNotNull { it.trim().toFloatOrNull() }
            return when (p.size) {
                2 * N -> CalibrationModel(p.subList(0, N).toFloatArray(), p.subList(N, 2 * N).toFloatArray())
                // Legacy affine string `ax,bx,cx,ay,by,cy` (prompt 031) stays loadable.
                6 -> affine(p[0], p[1], p[2], p[3], p[4], p[5])
                else -> null
            }
        }
    }
}

data class CalibrationSample(val gazeX: Float, val gazeY: Float, val screenX: Float, val screenY: Float)

/** A fitted calibration plus its held-out validation error in normalised-screen units (NaN if unknown). */
data class CalibrationFit(val model: CalibrationModel, val error: Float)

/**
 * Least-squares fit (gaze → normalised screen). Fits the full 2nd-order polynomial when there are
 * enough points; otherwise (or if that system is singular) falls back to an affine fit embedded in
 * the polynomial model, so calibration never regresses below the previous affine behaviour.
 */
object GazeCalibrator {
    fun fit(samples: List<CalibrationSample>): CalibrationModel? {
        if (samples.size < 3) return null
        if (samples.size >= CalibrationModel.N) {
            fitBasis(samples) { CalibrationModel.basis(it.gazeX, it.gazeY) }?.let { return it }
        }
        // Affine fallback: basis [1, gx, gy] (the polynomial basis's first three terms).
        return fitBasis(samples) { floatArrayOf(1f, it.gazeX, it.gazeY) }
    }

    /** Relative ridge applied to the polynomial curvature terms (prompt 039). */
    const val RIDGE = 0.1

    /**
     * Fit an **affine** and a **ridge-regularised polynomial** candidate and keep whichever has the
     * lower held-out validation error (prompt 039), so the richer model can never regress below
     * affine. With no validation points, falls back to the plain [fit]; its error is then NaN.
     */
    fun fitBest(fitSamples: List<CalibrationSample>, validation: List<CalibrationSample>): CalibrationFit? {
        if (validation.isEmpty()) return fit(fitSamples)?.let { CalibrationFit(it, Float.NaN) }
        val candidates = ArrayList<CalibrationModel>()
        fitBasis(fitSamples) { floatArrayOf(1f, it.gazeX, it.gazeY) }?.let { candidates.add(it) }
        if (fitSamples.size >= CalibrationModel.N) {
            fitBasis(fitSamples, RIDGE) { CalibrationModel.basis(it.gazeX, it.gazeY) }?.let { candidates.add(it) }
        }
        return candidates.map { CalibrationFit(it, meanError(it, validation)) }.minByOrNull { it.error }
    }

    private fun meanError(model: CalibrationModel, validation: List<CalibrationSample>): Float =
        validation.map { s ->
            val (px, py) = model.map(s.gazeX, s.gazeY)
            hypot((px - s.screenX).toDouble(), (py - s.screenY).toDouble()).toFloat()
        }.average().toFloat()

    /**
     * Solve the normal equations for both axes over [basis]; pad the result to the polynomial size.
     * [ridge] applies a relative penalty to the **curvature** terms (index >= 3) only — scaled to
     * their own magnitude, so it is units-independent and leaves the affine part unbiased — to damp
     * noise amplification (prompt 039).
     */
    private fun fitBasis(
        samples: List<CalibrationSample>,
        ridge: Double = 0.0,
        basis: (CalibrationSample) -> FloatArray,
    ): CalibrationModel? {
        val k = basis(samples[0]).size
        val ata = Array(k) { DoubleArray(k) }
        val atbx = DoubleArray(k)
        val atby = DoubleArray(k)
        for (s in samples) {
            val b = basis(s)
            for (i in 0 until k) {
                val bi = b[i].toDouble()
                for (j in 0 until k) ata[i][j] += bi * b[j]
                atbx[i] += bi * s.screenX
                atby[i] += bi * s.screenY
            }
        }
        if (ridge > 0.0 && k > 3) {
            var diag = 0.0
            for (i in 3 until k) diag += ata[i][i]
            val lambda = ridge * diag / (k - 3)
            for (i in 3 until k) ata[i][i] += lambda
        }
        val cx = solveLinear(ata, atbx) ?: return null
        val cy = solveLinear(ata, atby) ?: return null
        val px = FloatArray(CalibrationModel.N)
        val py = FloatArray(CalibrationModel.N)
        for (i in 0 until k) {
            px[i] = cx[i].toFloat()
            py[i] = cy[i].toFloat()
        }
        return CalibrationModel(px, py)
    }

    /** Gaussian elimination with partial pivoting; copies inputs, returns null if singular. */
    private fun solveLinear(aIn: Array<DoubleArray>, bIn: DoubleArray): DoubleArray? {
        val n = bIn.size
        val a = Array(n) { aIn[it].copyOf() }
        val b = bIn.copyOf()
        for (col in 0 until n) {
            var piv = col
            for (r in col + 1 until n) if (abs(a[r][col]) > abs(a[piv][col])) piv = r
            if (abs(a[piv][col]) < 1e-9) return null
            if (piv != col) {
                val tr = a[piv]; a[piv] = a[col]; a[col] = tr
                val tb = b[piv]; b[piv] = b[col]; b[col] = tb
            }
            val d = a[col][col]
            for (r in col + 1 until n) {
                val f = a[r][col] / d
                if (f == 0.0) continue
                for (c in col until n) a[r][c] -= f * a[col][c]
                b[r] -= f * b[col]
            }
        }
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = b[i]
            for (j in i + 1 until n) sum -= a[i][j] * x[j]
            x[i] = sum / a[i][i]
        }
        return x
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

    /** Mean held-out validation error in normalised-screen units (prompt 033); null if unknown. */
    private val _error = MutableStateFlow<Float?>(null)
    val error: StateFlow<Float?> = _error.asStateFlow()

    fun set(model: CalibrationModel?, error: Float? = null) {
        _state.value = model
        _error.value = error
    }

    fun clear() {
        _state.value = null
        _error.value = null
    }
}
