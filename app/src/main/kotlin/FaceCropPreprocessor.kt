package com.example.saccadacusandroid

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Builds the full-face gaze input (prompt 051): a 224×224×3 RGB face crop, ImageNet-normalised per
 * channel `(x/255 - mean)/std` (mean 0.485/0.456/0.406, std 0.229/0.224/0.225), row-major NHWC. Serves
 * UniGaze-B and other full-face models. Crop geometry lives in the pure [FaceCropGeometry].
 *
 * Note: this is a plain landmark face box, NOT the ETH-XGaze data-normalisation warp these models are
 * trained on, so accuracy is reduced vs the papers (partly recovered by calibration) — see docs.
 */
object FaceCropPreprocessor {
    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun facePatch(bitmap: Bitmap, xs: FloatArray, ys: FloatArray): FloatArray? {
        val box = FaceCropGeometry.faceCropBox(xs, ys, bitmap.width, bitmap.height) ?: return null
        val crop = try {
            Bitmap.createBitmap(bitmap, box.left, box.top, box.width, box.height)
        } catch (t: Throwable) {
            return null
        }
        val p = FaceCropGeometry.PATCH
        val scaled = Bitmap.createScaledBitmap(crop, p, p, true)
        if (scaled != crop) crop.recycle()
        val px = IntArray(p * p)
        scaled.getPixels(px, 0, p, 0, 0, p, p)
        scaled.recycle()
        val out = FloatArray(p * p * 3)
        var o = 0
        for (i in 0 until p * p) {
            val c = px[i]
            out[o++] = (Color.red(c) / 255f - MEAN[0]) / STD[0]
            out[o++] = (Color.green(c) / 255f - MEAN[1]) / STD[1]
            out[o++] = (Color.blue(c) / 255f - MEAN[2]) / STD[2]
        }
        return out
    }
}
