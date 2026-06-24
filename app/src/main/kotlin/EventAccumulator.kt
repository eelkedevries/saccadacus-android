package com.example.saccadacusandroid

import kotlin.math.abs

/**
 * Accumulates derived per-frame signals into a sliding window, periodically runs the
 * detectors, and maintains cumulative (de-duplicated) live event counts (prompt 006).
 * Fed from a single thread (the MediaPipe result callback).
 */
class EventAccumulator {
    private val saccadeSamples = ArrayDeque<SaccadeSample>()
    private val blinkSamples = ArrayDeque<BlinkSample>()
    private var sinceDetect = 0
    private var saccadeCount = 0L
    private var blinkCount = 0L
    private var lastSaccadeOnset = Long.MIN_VALUE
    private var lastBlinkOnset = Long.MIN_VALUE
    private var lastHeadYaw = Float.NaN
    private var lastHeadTsMs = 0L
    private var headLabel = "-"

    fun onResult(result: TrackingFrameResult, tsMs: Long) {
        val eye = result.leftEye ?: result.rightEye ?: return
        val isBlink = eye.blinkState == BlinkState.CLOSED || eye.blinkState == BlinkState.CLOSING
        val headStill = computeHeadStill(result.headPose, tsMs)
        headLabel = if (headStill) "head_still" else "head_moving"

        saccadeSamples.addLast(SaccadeSample(tsMs, eye.irisXLocal, eye.irisYLocal, eye.reliability, isBlink, headStill))
        blinkSamples.addLast(BlinkSample(tsMs, eye.blinkState))
        while (saccadeSamples.size > WINDOW) saccadeSamples.removeFirst()
        while (blinkSamples.size > WINDOW) blinkSamples.removeFirst()

        if (++sinceDetect >= DETECT_EVERY) {
            sinceDetect = 0
            runDetection()
        }
    }

    private fun computeHeadStill(pose: HeadPose?, tsMs: Long): Boolean {
        if (pose == null) return true
        val still = if (!lastHeadYaw.isNaN() && tsMs > lastHeadTsMs) {
            val dt = (tsMs - lastHeadTsMs) / 1000.0
            abs(pose.yawDeg - lastHeadYaw) / dt < HEAD_STILL_DEG_PER_S
        } else {
            true
        }
        lastHeadYaw = pose.yawDeg
        lastHeadTsMs = tsMs
        return still
    }

    private fun runDetection() {
        for (event in SaccadeDetector.detect(saccadeSamples.toList())) {
            if (event.onsetMs > lastSaccadeOnset) {
                lastSaccadeOnset = event.onsetMs
                saccadeCount++
            }
        }
        for (event in BlinkDetector.detect(blinkSamples.toList())) {
            if (event.onsetMs > lastBlinkOnset) {
                lastBlinkOnset = event.onsetMs
                blinkCount++
            }
        }
        EventStats.update(EventCounts(saccadeCount, blinkCount, headLabel))
    }

    fun reset() {
        saccadeSamples.clear()
        blinkSamples.clear()
        sinceDetect = 0
        saccadeCount = 0L
        blinkCount = 0L
        lastSaccadeOnset = Long.MIN_VALUE
        lastBlinkOnset = Long.MIN_VALUE
        lastHeadYaw = Float.NaN
        EventStats.clear()
    }

    companion object {
        const val WINDOW = 120
        const val DETECT_EVERY = 15
        const val HEAD_STILL_DEG_PER_S = 30.0
    }
}
