package com.example.saccadacusandroid

/**
 * The input contract a side-loaded gaze model expects, so the right preprocessing is built for it
 * (prompt 048). Detected from the model's input tensor shapes at load; an unrecognised model yields a
 * null profile and the CNN source falls back to iris (a wrong-shaped model is never mis-fed).
 */
enum class GazeModelProfile {
    /** Single input `[1,36,60,1]`: one histogram-equalised grayscale eye patch (the original contract). */
    EYE_GRAY,
}

/** Pure (Android-free, unit-testable) profile detection from a model's input tensor shapes (prompt 048). */
object GazeModelProfiles {
    /** Drop a leading batch dim of 1 so `[1,36,60,1]` and `[36,60,1]` compare equal. */
    fun dimsNoBatch(shape: IntArray): IntArray =
        if (shape.isNotEmpty() && shape[0] == 1) shape.copyOfRange(1, shape.size) else shape

    /** Pick the profile from the model's input shapes (batch dim optional), or null if unrecognised. */
    fun detect(inputShapes: List<IntArray>): GazeModelProfile? {
        if (inputShapes.size == 1 && dimsNoBatch(inputShapes[0]).contentEquals(intArrayOf(36, 60, 1))) {
            return GazeModelProfile.EYE_GRAY
        }
        return null
    }
}
