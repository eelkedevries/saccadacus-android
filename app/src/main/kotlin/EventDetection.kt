package com.example.saccadacusandroid

import kotlin.math.hypot

/** Saccade / blink / head-motion detection (prompt 006), thresholds per spec §Domain rules. */

data class SaccadeSample(
    val tsMs: Long,
    val xLocal: Float,
    val yLocal: Float,
    val reliability: Float,
    val isBlink: Boolean,
    val headStill: Boolean,
)

data class SaccadeEvent(
    val onsetMs: Long,
    val offsetMs: Long,
    val durationMs: Long,
    val amplitude: Double,
    val confidence: Double,
    val headMotionLabel: String,
)

data class BlinkSample(val tsMs: Long, val blinkState: BlinkState)

data class BlinkEvent(
    val onsetMs: Long,
    val offsetMs: Long,
    val durationMs: Long,
    val confidence: Float,
)

object SaccadeDetector {
    const val ONSET_SPEED = 1.0      // eye-width units / second
    const val OFFSET_SPEED = 0.4
    const val MIN_DURATION_MS = 8L
    const val MAX_DURATION_MS = 200L
    const val MIN_AMPLITUDE = 0.03

    fun detect(samples: List<SaccadeSample>): List<SaccadeEvent> {
        val n = samples.size
        if (n < 2) return emptyList()
        val speed = DoubleArray(n)
        for (k in 1 until n) {
            val dt = (samples[k].tsMs - samples[k - 1].tsMs) / 1000.0
            speed[k] = if (dt > 0) {
                val dx = (samples[k].xLocal - samples[k - 1].xLocal).toDouble()
                val dy = (samples[k].yLocal - samples[k - 1].yLocal).toDouble()
                hypot(dx, dy) / dt
            } else {
                0.0
            }
        }
        val events = ArrayList<SaccadeEvent>()
        var k = 1
        while (k < n) {
            if (speed[k] < ONSET_SPEED) {
                k++
                continue
            }
            var startIdx = k
            while (startIdx > 1 && speed[startIdx - 1] > OFFSET_SPEED) startIdx--
            var endIdx = k
            while (endIdx + 1 < n && speed[endIdx + 1] > OFFSET_SPEED) endIdx++
            val a = startIdx - 1
            val b = endIdx
            val onsetMs = samples[a].tsMs
            val offsetMs = samples[b].tsMs
            val durationMs = offsetMs - onsetMs
            val amplitude = hypot(
                (samples[b].xLocal - samples[a].xLocal).toDouble(),
                (samples[b].yLocal - samples[a].yLocal).toDouble(),
            )
            val blinkInRange = (a..b).any { samples[it].isBlink }
            if (!blinkInRange && durationMs in MIN_DURATION_MS..MAX_DURATION_MS && amplitude >= MIN_AMPLITUDE) {
                val meanReliability = (a..b).map { samples[it].reliability.toDouble() }.average()
                val headStill = (a..b).all { samples[it].headStill }
                val label = if (headStill) "saccade_head_still" else "saccade_during_head_movement"
                val confidence = (meanReliability * if (headStill) 1.0 else 0.6).coerceIn(0.0, 1.0)
                events.add(SaccadeEvent(onsetMs, offsetMs, durationMs, amplitude, confidence, label))
            }
            k = endIdx + 1
        }
        return events
    }
}

object BlinkDetector {
    private val BLINK_PHASES = setOf(BlinkState.CLOSING, BlinkState.CLOSED, BlinkState.OPENING)

    fun detect(samples: List<BlinkSample>, minDurationMs: Long = 0L): List<BlinkEvent> {
        val events = ArrayList<BlinkEvent>()
        val n = samples.size
        var i = 0
        while (i < n) {
            if (samples[i].blinkState !in BLINK_PHASES) {
                i++
                continue
            }
            val start = i
            var reachedClosed = false
            while (i < n && samples[i].blinkState in BLINK_PHASES) {
                if (samples[i].blinkState == BlinkState.CLOSED) reachedClosed = true
                i++
            }
            val end = i - 1
            val onsetMs = samples[start].tsMs
            val offsetMs = if (i < n) samples[i].tsMs else samples[end].tsMs
            val durationMs = offsetMs - onsetMs
            if (durationMs < minDurationMs) continue
            events.add(BlinkEvent(onsetMs, offsetMs, durationMs, if (reachedClosed) 0.9f else 0.6f))
        }
        return events
    }
}
