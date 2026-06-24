package com.example.saccadacusandroid

/** Derived per-frame signals (prompt 005). Eye coordinates are in eye-width units. */
enum class BlinkState { OPEN, CLOSING, CLOSED, OPENING, UNKNOWN }

enum class Selection { IRIS, PUPIL }

data class EyeFeature(
    val irisXLocal: Float,
    val irisYLocal: Float,
    val reliability: Float,
    val blinkState: BlinkState,
)

data class HeadPose(
    val yawDeg: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
)

data class TrackingFrameResult(
    val faceDetected: Boolean,
    val faceReliability: Float,
    /** Participant's left eye (after mirror handling). */
    val leftEye: EyeFeature?,
    /** Participant's right eye. */
    val rightEye: EyeFeature?,
    val headPose: HeadPose?,
    // pupilCentre intentionally absent in v1 — iris-centre only (spec §Domain rules).
)
