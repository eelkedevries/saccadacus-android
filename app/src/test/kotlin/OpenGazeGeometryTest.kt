package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure Open Gaze / Google-family geometry (prompt 050). */
class OpenGazeGeometryTest {

    @Test
    fun eyeCornerLmsReturnsEightNormalisedCoords() {
        val xs = FloatArray(400) { 0.5f }
        val ys = FloatArray(400) { 0.5f }
        xs[362] = 0.70f; ys[362] = 0.40f
        xs[263] = 0.80f; ys[263] = 0.42f
        xs[33] = 0.20f; ys[33] = 0.41f
        xs[133] = 0.30f; ys[133] = 0.43f
        val lms = OpenGazeGeometry.eyeCornerLms(xs, ys)
        assertNotNull(lms)
        lms!!
        assertEquals(8, lms.size)
        assertEquals(0.70f, lms[0], 1e-6f)
        assertEquals(0.40f, lms[1], 1e-6f)
        assertEquals(0.20f, lms[4], 1e-6f) // right eye outer x
    }

    @Test
    fun eyeCornerLmsRejectsOutOfRange() {
        assertNull(OpenGazeGeometry.eyeCornerLms(FloatArray(50), FloatArray(50)))
    }

    @Test
    fun eyeCropSquareIsSquareAndCentred() {
        val xs = FloatArray(400) { 0.5f }
        val ys = FloatArray(400) { 0.5f }
        xs[10] = 0.4f; ys[10] = 0.5f
        xs[11] = 0.6f; ys[11] = 0.5f
        val box = OpenGazeGeometry.eyeCropSquare(xs, ys, intArrayOf(10, 11), 100, 100)
        assertNotNull(box)
        box!!
        // width 0.2 -> 20px; half = 20*(0.5+0.4)=18; centred on (50,50). The box is squared to the
        // larger side; exact pixels jitter by 1 from float->int truncation, so assert ranges + squareness.
        assertEquals("squared to the larger side", box.width, box.height)
        assertTrue("left ~32 (got ${box.left})", box.left in 30..33)
        assertTrue("top ~32 (got ${box.top})", box.top in 30..33)
        assertTrue("width ~36 (got ${box.width})", box.width in 35..38)
    }

    @Test
    fun eyeCropSquareRejectsBadInput() {
        assertNull(OpenGazeGeometry.eyeCropSquare(FloatArray(5), FloatArray(5), intArrayOf(10), 100, 100))
        assertNull(OpenGazeGeometry.eyeCropSquare(FloatArray(400), FloatArray(400), intArrayOf(10), 0, 100))
    }
}
