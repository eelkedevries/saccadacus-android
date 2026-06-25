package com.example.saccadacusandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the One-Euro gaze filter (prompt 037). */
class OneEuroFilterTest {

    private val dtNanos = 83_000_000L // ~12 fps analysis cadence

    @Test
    fun firstSamplePassesThrough() {
        val f = OneEuroFilter()
        assertEquals(0.4f, f.filter(0.4f, 0L), 1e-6f)
    }

    @Test
    fun constantInputSettlesToConstant() {
        val f = OneEuroFilter()
        var t = 0L
        var out = 0f
        repeat(50) { out = f.filter(0.25f, t); t += dtNanos }
        assertEquals(0.25f, out, 1e-3f)
    }

    @Test
    fun reducesJitterVariance() {
        val f = OneEuroFilter()
        val mean = 0.3f
        val jitter = floatArrayOf(0.02f, -0.02f, 0.018f, -0.021f, 0.019f, -0.017f, 0.022f, -0.02f)
        var t = 0L
        repeat(10) { f.filter(mean, t); t += dtNanos } // warm up at the mean
        val inputs = ArrayList<Float>()
        val outputs = ArrayList<Float>()
        repeat(40) { i ->
            val x = mean + jitter[i % jitter.size]
            inputs.add(x)
            outputs.add(f.filter(x, t))
            t += dtNanos
        }
        assertTrue("filtered variance should be below input variance", variance(outputs) < variance(inputs))
    }

    @Test
    fun followsStep() {
        val f = OneEuroFilter()
        var t = 0L
        repeat(20) { f.filter(0.1f, t); t += dtNanos }
        var out = 0f
        repeat(40) { out = f.filter(0.8f, t); t += dtNanos }
        assertTrue("expected to approach the new level, got $out", out > 0.7f)
    }

    private fun variance(xs: List<Float>): Float {
        val m = xs.average().toFloat()
        return xs.map { (it - m) * (it - m) }.average().toFloat()
    }
}
