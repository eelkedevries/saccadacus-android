package com.example.saccadacusandroid

/**
 * The input contract a side-loaded gaze model expects, so the right preprocessing is built for it
 * (prompt 048). Detected from the model's input tensor shapes at load; an unrecognised model yields a
 * null profile and the CNN source falls back to iris (a wrong-shaped model is never mis-fed).
 */
enum class GazeModelProfile {
    /** Single input `[1,36,60,1]`: one histogram-equalised grayscale eye patch (the original contract). */
    EYE_GRAY,

    /**
     * WebEyeTrack / BlazeGaze (prompt 049): three inputs — `image` `[1,128,512,3]` RGB both-eyes strip,
     * `head_vector` `[1,3]`, `face_origin_3d` `[1,3]` — and a `[1,2]` point-of-gaze output.
     */
    WEB_EYE_TRACK,

    /**
     * Open Gaze / Google PoG family (prompt 050): two `[1,128,128,3]` RGB eye crops + an `[1,8]`
     * eye-corner landmark vector, output `[1,2]` point-of-gaze in centimetres.
     */
    DUAL_EYE_POG,

    /**
     * Full-face models (prompt 051): a single `[1,224,224,3]` RGB face crop, output `[1,2]` gaze
     * angles. Serves UniGaze-B / ETH-XGaze / MobileGaze / L2CS.
     */
    FULL_FACE,
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
        if (inputShapes.size == 1 && dimsNoBatch(inputShapes[0]).contentEquals(intArrayOf(224, 224, 3))) {
            return GazeModelProfile.FULL_FACE
        }
        if (inputShapes.size == 3) {
            val hasStrip = inputShapes.any { dimsNoBatch(it).contentEquals(intArrayOf(128, 512, 3)) }
            val vec3 = inputShapes.count { dimsNoBatch(it).contentEquals(intArrayOf(3)) }
            if (hasStrip && vec3 == 2) return GazeModelProfile.WEB_EYE_TRACK
            val eyeCrops = inputShapes.count {
                val d = dimsNoBatch(it)
                d.contentEquals(intArrayOf(128, 128, 3)) || d.contentEquals(intArrayOf(3, 128, 128))
            }
            val lms8 = inputShapes.count { dimsNoBatch(it).contentEquals(intArrayOf(8)) }
            if (eyeCrops == 2 && lms8 == 1) return GazeModelProfile.DUAL_EYE_POG
        }
        return null
    }
}
