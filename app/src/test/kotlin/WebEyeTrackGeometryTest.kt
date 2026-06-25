package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

/** Unit tests for the pure WebEyeTrack geometry (prompt 049). */
class WebEyeTrackGeometryTest {

    private val eps = 1e-4f

    @Test
    fun headVectorStraightAheadPointsAtCamera() {
        val v = WebEyeTrackGeometry.headVector(0f, 0f)
        assertEquals(0f, v[0], eps)
        assertEquals(0f, v[1], eps)
        assertEquals(-1f, v[2], eps)
    }

    @Test
    fun headVectorIsUnitLength() {
        val v = WebEyeTrackGeometry.headVector(0.3f, -0.2f)
        val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        assertEquals(1f, len, eps)
    }

    @Test
    fun eyeStripQuadAppliesRadialPadding() {
        val xs = FloatArray(400) { 0.5f }
        val ys = FloatArray(400) { 0.5f }
        xs[WebEyeTrackGeometry.L_TOP] = 0.4f
        ys[WebEyeTrackGeometry.L_TOP] = 0.4f
        val q = WebEyeTrackGeometry.eyeStripQuad(xs, ys, 100, 100)
        assertNotNull(q)
        q!!
        assertEquals(8, q.size)
        // L_TOP corner: px=40, cx=50 -> 40 + (40-50)*0.4 = 36 ; py=40, cy=50 -> 40 + (40-50)*0.2 = 38.
        assertEquals(36f, q[0], eps)
        assertEquals(38f, q[1], eps)
    }

    @Test
    fun eyeStripQuadRejectsOutOfRangeOrEmpty() {
        assertNull(WebEyeTrackGeometry.eyeStripQuad(FloatArray(50), FloatArray(50), 100, 100))
        assertNull(WebEyeTrackGeometry.eyeStripQuad(FloatArray(400), FloatArray(400), 0, 100))
    }

    @Test
    fun faceOriginDepthFromIpd() {
        // Eyes 0.2 of width apart at 100px width -> 20px IPD; z = 100 * 6.3 / 20 = 31.5 cm, centred.
        val o = WebEyeTrackGeometry.faceOrigin3d(0.4f, 0.5f, 0.6f, 0.5f, 100, 100)
        assertEquals(0f, o[0], 1e-3f)
        assertEquals(0f, o[1], 1e-3f)
        assertEquals(31.5f, o[2], 1e-3f)
    }

    @Test
    fun faceOriginFallsBackWhenEyesCoincide() {
        val o = WebEyeTrackGeometry.faceOrigin3d(0.5f, 0.5f, 0.5f, 0.5f, 100, 100)
        assertEquals(60f, o[2], 1e-3f)
    }
}
