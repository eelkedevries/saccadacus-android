package com.example.saccadacusandroid

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure (Android-free, unit-testable) geometry for the WebEyeTrack / BlazeGaze input profile (prompt
 * 049; RedForestAI, MIT code, arXiv:2508.19544). Reproduces the non-pixel parts of WebEyeTrack's
 * preprocessing as faithfully as the on-device signals allow; the pixel warp lives in the Android
 * [WebEyeTrackPreprocessor].
 *
 * Verified model contract (shipped `model.json`): three inputs — `image` `[1,128,512,3]` RGB in `[0,1]`
 * (both eyes, a homography-warped strip), `head_vector` `[1,3]` unit direction, `face_origin_3d`
 * `[1,3]` in centimetres — and output `[1,2]` = point-of-gaze in normalised screen coords (origin
 * centre, range ~`[-0.5,0.5]`). The few-shot MAML personalisation is replaced on-device by the app's
 * calibration (the model's raw PoG is fed through `GazeCalibrator` as the gaze signal).
 *
 * Honest caveats: `head_vector`'s Euler source and `face_origin_3d`'s metric reconstruction are
 * best-effort approximations of WebEyeTrack's pipeline (we lack camera intrinsics and the full metric
 * face mesh); calibration absorbs a consistent offset/scale, but verify accuracy on-device.
 */
object WebEyeTrackGeometry {
    // MediaPipe landmark indices for WebEyeTrack's eye-region warp (face-region quad + nose centre).
    const val L_TOP = 103
    const val L_BOTTOM = 150
    const val R_BOTTOM = 379
    const val R_TOP = 332
    const val NOSE = 4

    // Warped-square strip crop band (top/bottom landmarks), used by the Android preprocessor.
    const val STRIP_TOP = 151
    const val STRIP_BOTTOM = 195

    // Radial padding applied outward from the nose to the quad corners (x,y fractions).
    const val PAD_X = 0.4f
    const val PAD_Y = 0.2f

    // Nominal inter-pupillary distance (cm) for the metric depth estimate.
    const val IPD_CM = 6.3f

    /**
     * The four eye-region quad corners in pixels, order `L_TOP, L_BOTTOM, R_BOTTOM, R_TOP`, each pushed
     * radially out from the nose by [PAD_X]/[PAD_Y]. Returns flat `[x0,y0,x1,y1,x2,y2,x3,y3]`, or null
     * if a landmark index is out of range / NaN, or the image is empty.
     */
    fun eyeStripQuad(xs: FloatArray, ys: FloatArray, imgW: Int, imgH: Int): FloatArray? {
        if (imgW <= 0 || imgH <= 0) return null
        val corners = intArrayOf(L_TOP, L_BOTTOM, R_BOTTOM, R_TOP)
        for (i in intArrayOf(NOSE, L_TOP, L_BOTTOM, R_BOTTOM, R_TOP)) {
            if (i < 0 || i >= xs.size || i >= ys.size) return null
            if (xs[i].isNaN() || ys[i].isNaN()) return null
        }
        val cx = xs[NOSE] * imgW
        val cy = ys[NOSE] * imgH
        val out = FloatArray(8)
        for (k in 0 until 4) {
            val px = xs[corners[k]] * imgW
            val py = ys[corners[k]] * imgH
            out[k * 2] = px + (px - cx) * PAD_X
            out[k * 2 + 1] = py + (py - cy) * PAD_Y
        }
        return out
    }

    /**
     * WebEyeTrack's head-orientation unit vector from head Euler angles (radians). It reorders the
     * angles to `(pitch' = -yaw, yaw' = pitch)` and converts to a Cartesian direction
     * `x = cos(pitch')·sin(yaw'), y = sin(pitch'), z = -cos(pitch')·cos(yaw')`, then normalises.
     * Looking straight ahead (`yaw=pitch=0`) gives `[0,0,-1]` (toward the camera).
     */
    fun headVector(yawRad: Float, pitchRad: Float): FloatArray {
        val pitchP = -yawRad
        val yawP = pitchRad
        var x = cos(pitchP) * sin(yawP)
        var y = sin(pitchP)
        var z = -cos(pitchP) * cos(yawP)
        val n = sqrt(x * x + y * y + z * z)
        if (n > 1e-6f) { x /= n; y /= n; z /= n }
        return floatArrayOf(x, y, z)
    }

    /**
     * Best-effort metric face origin `[x,y,z]` in centimetres, camera at the origin. Depth `z` comes
     * from a pinhole back-projection of the inter-eye pixel distance against a nominal [IPD_CM] with
     * focal ≈ image width; `x,y` are the eye-midpoint back-projected at that depth. Inputs are the
     * left/right eye-centre **normalised** coordinates. Approximate (no true intrinsics) — calibration
     * absorbs a consistent offset.
     */
    fun faceOrigin3d(
        leyeXn: Float,
        leyeYn: Float,
        reyeXn: Float,
        reyeYn: Float,
        imgW: Int,
        imgH: Int,
    ): FloatArray {
        val focal = imgW.toFloat().coerceAtLeast(1f)
        val lx = leyeXn * imgW
        val ly = leyeYn * imgH
        val rx = reyeXn * imgW
        val ry = reyeYn * imgH
        val ipdPx = sqrt((lx - rx) * (lx - rx) + (ly - ry) * (ly - ry))
        val z = if (ipdPx > 1f) focal * IPD_CM / ipdPx else 60f
        val midX = (lx + rx) * 0.5f
        val midY = (ly + ry) * 0.5f
        val x = (midX - imgW * 0.5f) / focal * z
        val y = (midY - imgH * 0.5f) / focal * z
        return floatArrayOf(x, y, z)
    }
}
