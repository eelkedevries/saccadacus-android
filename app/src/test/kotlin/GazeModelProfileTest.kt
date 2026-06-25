package com.example.saccadacusandroid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for pure gaze-model input-profile detection (prompt 048). */
class GazeModelProfileTest {

    @Test
    fun detectsEyeGrayWithBatchDim() {
        assertEquals(
            GazeModelProfile.EYE_GRAY,
            GazeModelProfiles.detect(listOf(intArrayOf(1, 36, 60, 1))),
        )
    }

    @Test
    fun detectsEyeGrayWithoutBatchDim() {
        assertEquals(
            GazeModelProfile.EYE_GRAY,
            GazeModelProfiles.detect(listOf(intArrayOf(36, 60, 1))),
        )
    }

    @Test
    fun rejectsUnknownShapes() {
        assertNull(GazeModelProfiles.detect(listOf(intArrayOf(1, 128, 512, 3))))
        assertNull(GazeModelProfiles.detect(listOf(intArrayOf(1, 36, 60, 1), intArrayOf(1, 3))))
        assertNull(GazeModelProfiles.detect(emptyList()))
    }

    @Test
    fun dimsNoBatchStripsLeadingOne() {
        assertArrayEquals(intArrayOf(36, 60, 1), GazeModelProfiles.dimsNoBatch(intArrayOf(1, 36, 60, 1)))
        assertArrayEquals(intArrayOf(36, 60, 1), GazeModelProfiles.dimsNoBatch(intArrayOf(36, 60, 1)))
    }
}
