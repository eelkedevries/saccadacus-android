package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-JVM tests for the gaze calibration affine fit (prompt 031). */
class CalibrationFitTest {

    @Test
    fun recoversKnownAffine() {
        // gaze in [-1, 1] -> screen [0, 1]: screenX = 0.5*gx + 0.5; screenY = 0.5*gy + 0.5.
        val pts = listOf(
            -1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f, 0f to 0f,
        ).map { (gx, gy) -> CalibrationSample(gx, gy, 0.5f * gx + 0.5f, 0.5f * gy + 0.5f) }
        val m = GazeCalibrator.fit(pts)
        assertNotNull(m)
        m!!
        assertEquals(0.5, m.ax.toDouble(), 1e-3)
        assertEquals(0.0, m.bx.toDouble(), 1e-3)
        assertEquals(0.5, m.cx.toDouble(), 1e-3)
        assertEquals(0.0, m.ay.toDouble(), 1e-3)
        assertEquals(0.5, m.by.toDouble(), 1e-3)
        assertEquals(0.5, m.cy.toDouble(), 1e-3)
        val (sx, sy) = m.map(0f, 0f)
        assertEquals(0.5, sx.toDouble(), 1e-3)
        assertEquals(0.5, sy.toDouble(), 1e-3)
    }

    @Test
    fun returnsNullForTooFewPoints() {
        assertNull(
            GazeCalibrator.fit(
                listOf(CalibrationSample(0f, 0f, 0f, 0f), CalibrationSample(1f, 1f, 1f, 1f)),
            ),
        )
    }

    @Test
    fun returnsNullForDegenerateGaze() {
        // All points share the same gaze -> singular normal matrix.
        val pts = (0..4).map { CalibrationSample(0.2f, 0.3f, it * 0.1f, it * 0.1f) }
        assertNull(GazeCalibrator.fit(pts))
    }

    @Test
    fun serializeRoundTrip() {
        val m = CalibrationModel(1f, 2f, 3f, 4f, 5f, 6f)
        val restored = CalibrationModel.deserialize(m.serialize())
        assertNotNull(restored)
        assertEquals(m, restored)
    }
}
