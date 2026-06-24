package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Test

/** Parity / threshold tests for the ported event detectors (prompt 006). */
class EventDetectionTest {

    private fun saccadeSamples(xs: List<Float>, headStill: Boolean = true, blink: Boolean = false) =
        xs.mapIndexed { i, x -> SaccadeSample((i * 10).toLong(), x, 0f, 1f, blink, headStill) }

    @Test
    fun detectsSingleSaccade() {
        val xs = listOf(0f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f)
        val events = SaccadeDetector.detect(saccadeSamples(xs))
        assertEquals(1, events.size)
        assertEquals(0.1, events[0].amplitude, 1e-4)
        assertEquals(10L, events[0].durationMs)
        assertEquals("saccade_head_still", events[0].headMotionLabel)
    }

    @Test
    fun ignoresSubThresholdDrift() {
        val xs = (0 until 12).map { it * 0.001f }
        assertEquals(0, SaccadeDetector.detect(saccadeSamples(xs)).size)
    }

    @Test
    fun ignoresSaccadeOverlappingBlink() {
        val xs = listOf(0f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f)
        assertEquals(0, SaccadeDetector.detect(saccadeSamples(xs, blink = true)).size)
    }

    @Test
    fun labelsSaccadeDuringHeadMovement() {
        val xs = listOf(0f, 0f, 0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f)
        val events = SaccadeDetector.detect(saccadeSamples(xs, headStill = false))
        assertEquals(1, events.size)
        assertEquals("saccade_during_head_movement", events[0].headMotionLabel)
    }

    @Test
    fun detectsBlinkWithHighConfidence() {
        val states = listOf(
            BlinkState.OPEN, BlinkState.OPEN, BlinkState.CLOSING,
            BlinkState.CLOSED, BlinkState.OPENING, BlinkState.OPEN, BlinkState.OPEN,
        )
        val samples = states.mapIndexed { i, s -> BlinkSample((i * 10).toLong(), s) }
        val blinks = BlinkDetector.detect(samples)
        assertEquals(1, blinks.size)
        assertEquals(20L, blinks[0].onsetMs)
        assertEquals(50L, blinks[0].offsetMs)
        assertEquals(30L, blinks[0].durationMs)
        assertEquals(0.9f, blinks[0].confidence, 1e-4f)
    }

    @Test
    fun ringBufferVelocityAndOverwrite() {
        val buffer = FloatRingBuffer(2)
        buffer.add(1f)
        buffer.add(2f)
        buffer.add(3f)
        assertEquals(2, buffer.size)
        assertEquals(2f, buffer[0], 1e-6f)
        assertEquals(3f, buffer[1], 1e-6f)
        assertEquals(2.0, buffer.latestVelocity(0.5), 1e-6)
    }
}
