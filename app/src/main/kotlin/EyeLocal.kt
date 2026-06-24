package com.example.saccadacusandroid

import kotlin.math.hypot

data class Point2D(val x: Float, val y: Float)

data class EyeCorners(val leftCorner: Point2D, val rightCorner: Point2D)

data class EyeLocalPoint(val xLocal: Float, val yLocal: Float)

/**
 * Eye-local projection, ported line-for-line from the browser repo (spec §Domain rules).
 * Inputs must already be in a participant frame (x = participant right, y = up); the
 * adapter does that conversion. Positive xLocal = participant's right, positive yLocal =
 * up; corner distance is the eye-width normalisation; degenerate corners -> (0, 0).
 */
fun projectEyeLocal(corners: EyeCorners, feature: Point2D): EyeLocalPoint {
    val ux = corners.rightCorner.x - corners.leftCorner.x
    val uy = corners.rightCorner.y - corners.leftCorner.y
    val eyeWidth = hypot(ux, uy)
    if (eyeWidth == 0f) return EyeLocalPoint(0f, 0f)
    val uxn = ux / eyeWidth
    val uyn = uy / eyeWidth
    val vxn = -uyn
    val vyn = uxn
    val originX = (corners.leftCorner.x + corners.rightCorner.x) / 2f
    val originY = (corners.leftCorner.y + corners.rightCorner.y) / 2f
    val dx = feature.x - originX
    val dy = feature.y - originY
    return EyeLocalPoint(
        xLocal = (dx * uxn + dy * uyn) / eyeWidth,
        yLocal = (dx * vxn + dy * vyn) / eyeWidth,
    )
}

/**
 * MediaPipe Face Mesh landmark indices (478-landmark refined model). "left"/"right" here
 * are from the IMAGE perspective; participant eye identity depends on mirroring (see
 * [SignConvention]). Iris centres come from the iris refinement (468 / 473).
 */
object FaceMeshIndices {
    const val IMG_LEFT_EYE_CORNER_OUTER = 33
    const val IMG_LEFT_EYE_CORNER_INNER = 133
    const val IMG_LEFT_IRIS_CENTRE = 468
    const val IMG_RIGHT_EYE_CORNER_INNER = 362
    const val IMG_RIGHT_EYE_CORNER_OUTER = 263
    const val IMG_RIGHT_IRIS_CENTRE = 473
}

/**
 * Maps normalized image landmarks (x right, y DOWN, mirrored for the front camera) into
 * the participant frame (x = participant right, y = up).
 *
 * ON-DEVICE CALIBRATION: if a quick test shows a flipped axis or swapped eyes, flip the
 * single offending flag here — no other code changes. Expected after correct setup:
 * look right -> xLocal increases; look up -> yLocal increases; left eye labelled "left".
 */
object SignConvention {
    const val MIRROR_X = true
    const val FLIP_Y = true
}
