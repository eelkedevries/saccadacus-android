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
    fun detectsWebEyeTrack() {
        assertEquals(
            GazeModelProfile.WEB_EYE_TRACK,
            GazeModelProfiles.detect(
                listOf(intArrayOf(1, 128, 512, 3), intArrayOf(1, 3), intArrayOf(1, 3)),
            ),
        )
    }

    @Test
    fun detectsFullFace() {
        assertEquals(
            GazeModelProfile.FULL_FACE,
            GazeModelProfiles.detect(listOf(intArrayOf(1, 224, 224, 3))),
        )
    }

    @Test
    fun detectsDualEyePog() {
        assertEquals(
            GazeModelProfile.DUAL_EYE_POG,
            GazeModelProfiles.detect(
                listOf(intArrayOf(1, 128, 128, 3), intArrayOf(1, 128, 128, 3), intArrayOf(1, 8)),
            ),
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
