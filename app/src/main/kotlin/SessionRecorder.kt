package com.example.saccadacusandroid

import java.util.Collections

/**
 * In-memory recording of a session (prompt 007): per-frame derived samples, motion-sensor
 * samples (kept in their own timestamp domain), tracking-loss intervals, and metadata.
 * Holds derived signals only — never raw images. Incremental disk export is prompt 008.
 */
object SessionRecorder {

    data class Sample(
        val tElapsedNanos: Long,
        val leftX: Float, val leftY: Float, val leftReliability: Float, val leftBlink: BlinkState,
        val rightX: Float, val rightY: Float, val rightReliability: Float, val rightBlink: BlinkState,
        val yaw: Float, val pitch: Float, val roll: Float,
        val faceReliability: Float,
    )

    data class SensorSample(val sensor: String, val tNanos: Long, val v0: Float, val v1: Float, val v2: Float)

    data class LossInterval(val startNanos: Long, var endNanos: Long)

    @Volatile var profileName: String = ""
    @Volatile var startWallClockMs: Long = 0L
    @Volatile var startElapsedNanos: Long = 0L
    @Volatile var stopWallClockMs: Long = 0L

    val samples: MutableList<Sample> = Collections.synchronizedList(ArrayList())
    val sensorSamples: MutableList<SensorSample> = Collections.synchronizedList(ArrayList())
    val lossIntervals: MutableList<LossInterval> = Collections.synchronizedList(ArrayList())
    private var currentLoss: LossInterval? = null

    fun start(profile: String, wallClockMs: Long, elapsedNanos: Long) {
        clear()
        profileName = profile
        startWallClockMs = wallClockMs
        startElapsedNanos = elapsedNanos
    }

    fun addSample(frame: TrackingFrameResult, tElapsedNanos: Long) {
        val l = frame.leftEye
        val r = frame.rightEye
        samples.add(
            Sample(
                tElapsedNanos,
                l?.irisXLocal ?: Float.NaN, l?.irisYLocal ?: Float.NaN, l?.reliability ?: 0f, l?.blinkState ?: BlinkState.UNKNOWN,
                r?.irisXLocal ?: Float.NaN, r?.irisYLocal ?: Float.NaN, r?.reliability ?: 0f, r?.blinkState ?: BlinkState.UNKNOWN,
                frame.headPose?.yawDeg ?: Float.NaN, frame.headPose?.pitchDeg ?: Float.NaN, frame.headPose?.rollDeg ?: Float.NaN,
                frame.faceReliability,
            ),
        )
        if (!frame.faceDetected) {
            val loss = currentLoss
            if (loss == null) {
                val started = LossInterval(tElapsedNanos, tElapsedNanos)
                currentLoss = started
                lossIntervals.add(started)
            } else {
                loss.endNanos = tElapsedNanos
            }
        } else {
            currentLoss = null
        }
    }

    fun addSensorSample(sensor: String, tNanos: Long, v0: Float, v1: Float, v2: Float) {
        sensorSamples.add(SensorSample(sensor, tNanos, v0, v1, v2))
    }

    fun stop(wallClockMs: Long) {
        stopWallClockMs = wallClockMs
        currentLoss = null
    }

    fun clear() {
        samples.clear()
        sensorSamples.clear()
        lossIntervals.clear()
        currentLoss = null
        stopWallClockMs = 0L
    }
}
