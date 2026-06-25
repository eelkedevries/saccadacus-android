package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM tests for the fixation detector (prompt 028). */
class FixationDetectorTest {

    private fun sample(ts: Long, x: Float, y: Float = 0f, blink: Boolean = false) =
        SaccadeSample(ts, x, y, 1f, blink, true)

    @Test
    fun detectsStableFixationThenSaccade() {
        // 11 stable samples over 200 ms (0..200), then a large jump (a saccade) at 220 ms.
        val stable = (0..10).map { sample((it * 20).toLong(), 0f) }
        val jump = listOf(sample(220, 0.5f))
        val events = FixationDetector.detect(stable + jump)
        assertEquals(1, events.size)
        assertEquals(0L, events[0].onsetMs)
        assertEquals(200L, events[0].offsetMs)
        assertEquals(200L, events[0].durationMs)
    }

    @Test
    fun ignoresTooShortRun() {
        // Only 60 ms stable (< 100 ms minimum), then a jump.
        val stable = (0..3).map { sample((it * 20).toLong(), 0f) } // ts 0, 20, 40, 60
        val jump = listOf(sample(80, 0.5f))
        assertEquals(0, FixationDetector.detect(stable + jump).size)
    }

    @Test
    fun blinkBreaksFixation() {
        // Stable, a blink, then stable again — each run too short to qualify on its own.
        val before = (0..2).map { sample((it * 20).toLong(), 0f) }          // ts 0, 20, 40
        val blink = listOf(sample(60, 0f, blink = true))
        val after = (0..2).map { sample((80 + it * 20).toLong(), 0f) }      // ts 80, 100, 120
        assertEquals(0, FixationDetector.detect(before + blink + after).size)
    }
}
