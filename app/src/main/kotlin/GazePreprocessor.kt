package com.example.saccadacusandroid

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Builds the gaze CNN input (prompt 041): a normalised single-eye patch as a flat `36×60` grayscale
 * `[0,1]` float buffer (the `[1,36,60,1]` tensor), from the upright bitmap + MediaPipe normalised
 * landmark arrays, using [GazeGeometry] for the crop. Android-only (real `Bitmap`), so the crop
 * geometry lives in the pure, unit-tested [GazeGeometry]. Inference is added in prompt 042.
 */
object GazePreprocessor {
    /** The `[1,36,60,1]` input as a flat row-major `PATCH_H×PATCH_W` buffer, or null if no crop. */
    fun eyePatch(bitmap: Bitmap, xs: FloatArray, ys: FloatArray, eyeIndices: IntArray): FloatArray? {
        val box = GazeGeometry.eyeCrop(xs, ys, eyeIndices, bitmap.width, bitmap.height) ?: return null
        val crop = try {
            Bitmap.createBitmap(bitmap, box.left, box.top, box.width, box.height)
        } catch (t: Throwable) {
            return null
        }
        val scaled = Bitmap.createScaledBitmap(crop, GazeGeometry.PATCH_W, GazeGeometry.PATCH_H, true)
        if (scaled != crop) crop.recycle()
        val n = GazeGeometry.PATCH_W * GazeGeometry.PATCH_H
        val px = IntArray(n)
        scaled.getPixels(px, 0, GazeGeometry.PATCH_W, 0, 0, GazeGeometry.PATCH_W, GazeGeometry.PATCH_H)
        scaled.recycle()
        // Rec. 601 luma as an 8-bit grayscale buffer, histogram-equalised (MPIIGaze-style contrast
        // normalisation; also lifts low-light contrast), then normalised to [0,1] (prompt 047).
        val gray = IntArray(n)
        for (i in 0 until n) {
            val c = px[i]
            val lum = 0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)
            gray[i] = Math.round(lum).coerceIn(0, 255)
        }
        val eq = GazeImageOps.equalizeHist(gray)
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = eq[i] / 255f
        return out
    }
}
