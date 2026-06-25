package com.example.saccadacusandroid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure histogram-equalisation op (prompt 047). */
class GazeImageOpsTest {

    @Test
    fun emptyReturnsEmpty() {
        assertEquals(0, GazeImageOps.equalizeHist(IntArray(0)).size)
    }

    @Test
    fun flatImageIsUnchanged() {
        // A single intensity has no spread to equalise; cv2.equalizeHist leaves it untouched.
        assertArrayEquals(
            intArrayOf(100, 100, 100),
            GazeImageOps.equalizeHist(intArrayOf(100, 100, 100)),
        )
    }

    @Test
    fun rampMapsToKnownCdf() {
        // Five distinct levels, each count 1: cdf = 1,2,3,4,5; cdfMin = 1; denom = 4.
        // round((cdf-1)/4*255) -> 0, 64, 128, 191, 255.
        assertArrayEquals(
            intArrayOf(0, 64, 128, 191, 255),
            GazeImageOps.equalizeHist(intArrayOf(0, 64, 128, 192, 255)),
        )
    }

    @Test
    fun stretchesToFullRangeEndpoints() {
        val out = GazeImageOps.equalizeHist(intArrayOf(40, 80, 120, 160))
        assertEquals("min maps to 0", 0, out.first())
        assertEquals("max maps to 255", 255, out.last())
    }

    @Test
    fun preservesOrderAndStaysInRange() {
        val input = intArrayOf(10, 10, 50, 90, 200, 200, 255)
        val out = GazeImageOps.equalizeHist(input)
        for (v in out) assertTrue("in range: $v", v in 0..255)
        for (i in 1 until out.size) {
            if (input[i] >= input[i - 1]) assertTrue("monotonic at $i", out[i] >= out[i - 1])
        }
    }
}
