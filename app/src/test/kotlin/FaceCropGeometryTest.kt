package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Unit tests for the pure full-face crop geometry (prompt 051). */
class FaceCropGeometryTest {

    @Test
    fun squareBoxOverLandmarks() {
        val xs = floatArrayOf(0.3f, 0.7f, 0.5f)
        val ys = floatArrayOf(0.4f, 0.4f, 0.8f)
        val box = FaceCropGeometry.faceCropBox(xs, ys, 100, 100)
        assertNotNull(box)
        box!!
        // Squared to the larger side; allow 1px from float->int truncation.
        assertTrue("near-square (${box.width}x${box.height})", abs(box.width - box.height) <= 1)
        assertTrue("inside image", box.left >= 0 && box.top >= 0)
        assertTrue("has area", box.width > 0 && box.height > 0)
    }

    @Test
    fun rejectsEmptyOrBadImage() {
        assertNull(FaceCropGeometry.faceCropBox(FloatArray(0), FloatArray(0), 100, 100))
        assertNull(FaceCropGeometry.faceCropBox(floatArrayOf(0.5f), floatArrayOf(0.5f), 0, 100))
    }

    @Test
    fun skipsNaNLandmarks() {
        val xs = floatArrayOf(Float.NaN, 0.4f, 0.6f)
        val ys = floatArrayOf(Float.NaN, 0.4f, 0.6f)
        assertNotNull(FaceCropGeometry.faceCropBox(xs, ys, 100, 100))
    }
}
