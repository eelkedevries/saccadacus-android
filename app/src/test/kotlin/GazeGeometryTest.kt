package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the pure eye-crop geometry of the gaze CNN preprocessing (prompt 041). */
class GazeGeometryTest {

    private fun landmarks(): Pair<FloatArray, FloatArray> = Pair(FloatArray(478), FloatArray(478))

    @Test
    fun cropBoxMatchesExpectedBounds() {
        val (xs, ys) = landmarks()
        // Right-eye landmarks spanning x 0.30..0.40, y 0.45..0.55 on a 1000×1000 image.
        mapOf(
            33 to (0.30f to 0.50f), 133 to (0.40f to 0.50f), 159 to (0.35f to 0.45f),
            145 to (0.35f to 0.55f), 158 to (0.38f to 0.46f), 153 to (0.38f to 0.54f),
        ).forEach { (i, p) -> xs[i] = p.first; ys[i] = p.second }
        val box = GazeGeometry.eyeCrop(xs, ys, GazeGeometry.RIGHT_EYE, 1000, 1000)!!
        // eye width 100px, centre x 350, halfW = 100*(0.5+0.6)=110 -> left 240, width 220
        assertEquals(240, box.left)
        assertEquals(220, box.width)
        // halfH = 110*0.6 = 66, centre y 500 -> top 434, height 132
        assertEquals(434, box.top)
        assertEquals(132, box.height)
    }

    @Test
    fun cropBoxKeepsPatchAspect() {
        val (xs, ys) = landmarks()
        GazeGeometry.RIGHT_EYE.forEach { xs[it] = 0.5f; ys[it] = 0.5f }
        xs[33] = 0.45f; xs[133] = 0.55f
        val box = GazeGeometry.eyeCrop(xs, ys, GazeGeometry.RIGHT_EYE, 600, 600)!!
        // width:height should match 60:36 (= 5:3) within rounding
        assertEquals(GazeGeometry.PATCH_H.toDouble() / GazeGeometry.PATCH_W, box.height.toDouble() / box.width, 0.05)
    }

    @Test
    fun clampsToImageEdge() {
        val (xs, ys) = landmarks()
        GazeGeometry.RIGHT_EYE.forEach { xs[it] = 0.02f; ys[it] = 0.5f }
        xs[33] = 0.0f; xs[133] = 0.04f
        val box = GazeGeometry.eyeCrop(xs, ys, GazeGeometry.RIGHT_EYE, 100, 100)!!
        assertEquals(0, box.left)
        assertTrue(box.width > 0)
    }

    @Test
    fun nullForOutOfRangeIndex() {
        val (xs, ys) = landmarks()
        assertNull(GazeGeometry.eyeCrop(xs, ys, intArrayOf(0, 999), 100, 100))
    }

    @Test
    fun nullForNaNLandmark() {
        val (xs, ys) = landmarks()
        GazeGeometry.RIGHT_EYE.forEach { xs[it] = 0.4f; ys[it] = 0.5f }
        xs[33] = Float.NaN
        assertNull(GazeGeometry.eyeCrop(xs, ys, GazeGeometry.RIGHT_EYE, 100, 100))
    }

    @Test
    fun nullForEmptyImage() {
        val (xs, ys) = landmarks()
        GazeGeometry.RIGHT_EYE.forEach { xs[it] = 0.4f; ys[it] = 0.5f }
        assertNull(GazeGeometry.eyeCrop(xs, ys, GazeGeometry.RIGHT_EYE, 0, 0))
    }
}
