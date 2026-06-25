package com.example.saccadacusandroid

import org.junit.Assert.assertTrue
import org.junit.Test

/** Sanity tests for the in-app gaze-model reference list (prompt 045). */
class GazeModelsTest {

    @Test
    fun referenceListIsPopulated() {
        assertTrue("expected a sizeable reference list", GazeModels.all.size >= 10)
    }

    @Test
    fun everyEntryIsWellFormed() {
        for (m in GazeModels.all) {
            assertTrue("name blank", m.name.isNotBlank())
            assertTrue("licence blank for ${m.name}", m.license.isNotBlank())
            assertTrue("pros blank for ${m.name}", m.pros.isNotBlank())
            assertTrue("cons blank for ${m.name}", m.cons.isNotBlank())
        }
    }
}
