package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the adaptive per-eye blink-state classifier (prompt 034). */
class BlinkClassifierTest {

    @Test
    fun stableElevatedBaselineReadsOpen() {
        val c = BlinkClassifier()
        // An open eye whose eyeBlink score sits at an elevated but stable 0.35 must read OPEN,
        // not "closing" as the old fixed 0.2 threshold would have made it.
        repeat(30) { c.classifyLeft(0.35f) }
        assertEquals(BlinkState.OPEN, c.classifyLeft(0.35f))
    }

    @Test
    fun clearRiseReadsClosed() {
        val c = BlinkClassifier()
        repeat(30) { c.classifyLeft(0.30f) }      // settle the baseline at 0.30
        assertEquals(BlinkState.CLOSED, c.classifyLeft(0.95f))
    }

    @Test
    fun midRiseReadsClosing() {
        val c = BlinkClassifier()
        repeat(30) { c.classifyLeft(0.30f) }
        // 0.30 baseline + ~0.32 rise: above CLOSING_DELTA (0.25), below CLOSED_DELTA (0.5).
        assertEquals(BlinkState.CLOSING, c.classifyLeft(0.62f))
    }

    @Test
    fun blinkDoesNotInflateBaseline() {
        val c = BlinkClassifier()
        repeat(30) { c.classifyLeft(0.30f) }
        // A blink spike, then straight back to the open level: the open frame must still read
        // OPEN, proving the spike did not raise the baseline.
        c.classifyLeft(0.95f)
        c.classifyLeft(0.95f)
        assertEquals(BlinkState.OPEN, c.classifyLeft(0.30f))
    }

    @Test
    fun leftAndRightBaselinesAreIndependent() {
        val c = BlinkClassifier()
        repeat(30) { c.classifyLeft(0.40f) }      // left baseline high
        repeat(30) { c.classifyRight(0.05f) }     // right baseline low
        assertEquals(BlinkState.OPEN, c.classifyLeft(0.40f))
        // The same absolute 0.40 score on the right (low baseline) is a clear rise -> not open.
        assertEquals(BlinkState.CLOSING, c.classifyRight(0.40f))
    }

    @Test
    fun resetClearsState() {
        val c = BlinkClassifier()
        repeat(30) { c.classifyLeft(0.40f) }
        c.reset()
        // After reset, the first score becomes the new baseline (warm-up) and reads OPEN.
        assertEquals(BlinkState.OPEN, c.classifyLeft(0.05f))
        // A clear rise above that fresh 0.05 baseline now reads CLOSED.
        assertEquals(BlinkState.CLOSED, c.classifyLeft(0.95f))
    }
}
