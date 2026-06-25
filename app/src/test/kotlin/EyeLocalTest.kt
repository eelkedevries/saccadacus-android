package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the eye-local projection (prompt 028). */
class EyeLocalTest {

    // A horizontal eye one unit wide: left corner at the origin, right corner at (1, 0).
    private val corners = EyeCorners(Point2D(0f, 0f), Point2D(1f, 0f))

    @Test
    fun centredIrisIsZero() {
        val local = projectEyeLocal(corners, Point2D(0.5f, 0f))
        assertEquals(0.0, local.xLocal.toDouble(), 1e-5)
        assertEquals(0.0, local.yLocal.toDouble(), 1e-5)
    }

    @Test
    fun irisTowardRightCornerIncreasesX() {
        val local = projectEyeLocal(corners, Point2D(0.75f, 0f))
        assertEquals(0.25, local.xLocal.toDouble(), 1e-5)
        assertEquals(0.0, local.yLocal.toDouble(), 1e-5)
    }

    @Test
    fun irisAboveMidlineIncreasesY() {
        val local = projectEyeLocal(corners, Point2D(0.5f, 0.2f))
        assertEquals(0.0, local.xLocal.toDouble(), 1e-5)
        assertEquals(0.2, local.yLocal.toDouble(), 1e-5)
    }

    @Test
    fun degenerateCornersReturnZero() {
        val degenerate = EyeCorners(Point2D(0.3f, 0.3f), Point2D(0.3f, 0.3f))
        val local = projectEyeLocal(degenerate, Point2D(0.9f, 0.9f))
        assertEquals(0.0, local.xLocal.toDouble(), 1e-9)
        assertEquals(0.0, local.yLocal.toDouble(), 1e-9)
    }
}
