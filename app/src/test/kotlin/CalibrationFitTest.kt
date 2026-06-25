package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for the polynomial gaze calibration fit (prompts 031, 038). */
class CalibrationFitTest {

    private fun gridSamples(
        gxs: List<Float>,
        gys: List<Float>,
        fx: (Float, Float) -> Float,
        fy: (Float, Float) -> Float,
    ): List<CalibrationSample> =
        gxs.flatMap { gx -> gys.map { gy -> CalibrationSample(gx, gy, fx(gx, gy), fy(gx, gy)) } }

    private val quadX = { gx: Float, gy: Float ->
        0.5f + 1.2f * gx + 0.1f * gy + 0.3f * gx * gy - 0.2f * gx * gx + 0.05f * gy * gy
    }
    private val quadY = { gx: Float, gy: Float ->
        0.4f + 0.05f * gx + 1.1f * gy - 0.15f * gx * gy + 0.1f * gx * gx - 0.25f * gy * gy
    }

    @Test
    fun recoversKnownQuadratic() {
        val pts = gridSamples(listOf(-1f, 0f, 1f), listOf(-1f, 0f, 1f), quadX, quadY) // 9 points >= 6
        val m = GazeCalibrator.fit(pts)
        assertNotNull(m); m!!
        for (gx in listOf(-1f, -0.3f, 0.4f, 1f)) {
            for (gy in listOf(-1f, 0.2f, 1f)) {
                val (sx, sy) = m.map(gx, gy)
                assertEquals(quadX(gx, gy).toDouble(), sx.toDouble(), 1e-2)
                assertEquals(quadY(gx, gy).toDouble(), sy.toDouble(), 1e-2)
            }
        }
    }

    @Test
    fun affineFallbackWithFewPoints() {
        // 4 points (< 6): the fit falls back to affine and reproduces an affine target.
        val fx = { gx: Float, _: Float -> 0.5f * gx + 0.5f }
        val fy = { _: Float, gy: Float -> 0.5f * gy + 0.5f }
        val pts = listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)
            .map { (gx, gy) -> CalibrationSample(gx, gy, fx(gx, gy), fy(gx, gy)) }
        val m = GazeCalibrator.fit(pts)
        assertNotNull(m); m!!
        val (sx, sy) = m.map(0f, 0f)
        assertEquals(0.5, sx.toDouble(), 1e-3)
        assertEquals(0.5, sy.toDouble(), 1e-3)
        // Higher-order coefficients stay zero in the affine fallback.
        assertEquals(0f, m.cx[3], 1e-6f)
        assertEquals(0f, m.cx[4], 1e-6f)
        assertEquals(0f, m.cx[5], 1e-6f)
    }

    @Test
    fun returnsNullForTooFewPoints() {
        assertNull(
            GazeCalibrator.fit(listOf(CalibrationSample(0f, 0f, 0f, 0f), CalibrationSample(1f, 1f, 1f, 1f))),
        )
    }

    @Test
    fun returnsNullForDegenerateGaze() {
        // All points share the same gaze -> singular for both the polynomial and affine systems.
        val pts = (0..7).map { CalibrationSample(0.2f, 0.3f, it * 0.1f, it * 0.1f) }
        assertNull(GazeCalibrator.fit(pts))
    }

    @Test
    fun serializeRoundTrip() {
        val m = GazeCalibrator.fit(gridSamples(listOf(-1f, 0f, 1f), listOf(-1f, 0f, 1f), quadX, quadY))!!
        val restored = CalibrationModel.deserialize(m.serialize())
        assertNotNull(restored); restored!!
        val (ax, ay) = m.map(0.3f, -0.2f)
        val (bx, by) = restored.map(0.3f, -0.2f)
        assertEquals(ax.toDouble(), bx.toDouble(), 1e-5)
        assertEquals(ay.toDouble(), by.toDouble(), 1e-5)
    }

    @Test
    fun legacyAffineStringDeserialises() {
        // Old persisted format `ax,bx,cx,ay,by,cy` must still load (x = ax*gx + bx*gy + cx).
        val m = CalibrationModel.deserialize("1.2,0.1,0.5,0.05,1.1,0.4")
        assertNotNull(m); m!!
        val (sx, sy) = m.map(0.2f, 0.3f)
        assertEquals((1.2f * 0.2f + 0.1f * 0.3f + 0.5f).toDouble(), sx.toDouble(), 1e-5)
        assertEquals((0.05f * 0.2f + 1.1f * 0.3f + 0.4f).toDouble(), sy.toDouble(), 1e-5)
    }

    // --- Model selection by held-out validation (prompt 039) ---

    @Test
    fun fitBestChoosesPolynomialOnCurvedData() {
        val fx = { gx: Float, gy: Float -> 0.5f + 0.3f * gx + 0.4f * gx * gx + 0.2f * gy * gy }
        val fy = { gx: Float, gy: Float -> 0.5f + 0.3f * gy - 0.3f * gx * gx }
        val fitPts = gridSamples(listOf(-1f, 0f, 1f), listOf(-1f, 0f, 1f), fx, fy)
        val valPts = listOf(0.5f to -0.5f, -0.5f to 0.5f)
            .map { (gx, gy) -> CalibrationSample(gx, gy, fx(gx, gy), fy(gx, gy)) }
        val best = GazeCalibrator.fitBest(fitPts, valPts)
        assertNotNull(best); best!!
        // Affine cannot fit this curvature (its held-out error is ~0.28); the ridge-poly is ~0.06.
        assertTrue("polynomial should win on curved data, error=${best.error}", best.error < 0.1f)
        assertTrue("chosen model should carry curvature", best.model.cx[4] > 0.1f)
    }

    @Test
    fun fitBestMatchesAffineDataExactly() {
        // Affine source: the affine candidate fits with ~0 held-out error, so selection never regresses.
        val fx = { gx: Float, gy: Float -> 0.3f * gx + 0.1f * gy + 0.5f }
        val fy = { gx: Float, gy: Float -> 0.05f * gx + 0.4f * gy + 0.45f }
        val fitPts = gridSamples(listOf(-1f, 0f, 1f), listOf(-1f, 0f, 1f), fx, fy)
        val valPts = listOf(0.4f to -0.6f, -0.7f to 0.3f)
            .map { (gx, gy) -> CalibrationSample(gx, gy, fx(gx, gy), fy(gx, gy)) }
        val best = GazeCalibrator.fitBest(fitPts, valPts)!!
        assertEquals(0.0, best.error.toDouble(), 1e-3)
    }

    @Test
    fun fitBestEmptyValidationFallsBack() {
        val pts = gridSamples(listOf(-1f, 0f, 1f), listOf(-1f, 0f, 1f), quadX, quadY)
        val best = GazeCalibrator.fitBest(pts, emptyList())
        assertNotNull(best); best!!
        assertTrue(best.error.isNaN())
    }

    @Test
    fun fitBestNullForDegenerate() {
        val fitPts = (0..7).map { CalibrationSample(0.2f, 0.3f, it * 0.1f, it * 0.1f) }
        val valPts = listOf(CalibrationSample(0.2f, 0.3f, 0.5f, 0.5f))
        assertNull(GazeCalibrator.fitBest(fitPts, valPts))
    }
}
