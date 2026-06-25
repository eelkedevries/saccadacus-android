package com.example.saccadacusandroid

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Builds an Open Gaze / Google-family eye input (prompt 050): a `128×128×3` RGB eye crop, optionally
 * horizontally flipped (the left eye), per-channel normalised `(x/255 - mean)/std` with gaze-track's
 * mean `(0.3741,0.4076,0.5425)` and the distinctive small std `(0.02,0.02,0.02)`. Android-only (real
 * `Bitmap`); the crop geometry lives in the pure [OpenGazeGeometry]. Output is row-major NHWC
 * (`128×128×3`) — convert your model to NHWC (the onnx2tf default).
 */
object OpenGazePreprocessor {
    private val MEAN = floatArrayOf(0.3741f, 0.4076f, 0.5425f)
    private val STD = floatArrayOf(0.02f, 0.02f, 0.02f)

    fun eyePatchRgb(
        bitmap: Bitmap,
        xs: FloatArray,
        ys: FloatArray,
        indices: IntArray,
        flip: Boolean,
    ): FloatArray? {
        val box = OpenGazeGeometry.eyeCropSquare(xs, ys, indices, bitmap.width, bitmap.height) ?: return null
        val crop = try {
            Bitmap.createBitmap(bitmap, box.left, box.top, box.width, box.height)
        } catch (t: Throwable) {
            return null
        }
        val p = OpenGazeGeometry.PATCH
        val scaled = Bitmap.createScaledBitmap(crop, p, p, true)
        if (scaled != crop) crop.recycle()
        val px = IntArray(p * p)
        scaled.getPixels(px, 0, p, 0, 0, p, p)
        scaled.recycle()

        val out = FloatArray(p * p * 3)
        var o = 0
        for (y in 0 until p) {
            for (x in 0 until p) {
                val sx = if (flip) (p - 1 - x) else x
                val c = px[y * p + sx]
                out[o++] = (Color.red(c) / 255f - MEAN[0]) / STD[0]
                out[o++] = (Color.green(c) / 255f - MEAN[1]) / STD[1]
                out[o++] = (Color.blue(c) / 255f - MEAN[2]) / STD[2]
            }
        }
        return out
    }
}
