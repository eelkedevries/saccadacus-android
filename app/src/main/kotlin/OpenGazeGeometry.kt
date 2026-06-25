package com.example.saccadacusandroid

/**
 * Pure (Android-free, unit-testable) geometry for the Open Gaze / Google point-of-gaze family (prompt
 * 050) — the SAGE/iTracker-style model recreated by `DSSR2/gaze-track` (the runnable twin of the
 * withdrawn "Open Gaze", arXiv:2308.13495; also catalogue #7/#8). Computes the per-eye square crop box
 * and the 8 eye-corner landmark coordinates; the pixel sampling lives in the Android
 * [OpenGazePreprocessor].
 *
 * Verified contract (from gaze-track): inputs are `leftEye`/`rightEye` `[1,3,128,128]` RGB (left
 * horizontally flipped, per-channel `(x/255 - mean)/std` with mean `(0.3741,0.4076,0.5425)`, std
 * `(0.02,0.02,0.02)`) and `lms` `[1,8]` eye-corner coordinates normalised by frame size; output `[1,2]`
 * is a point-of-gaze in **centimetres** (camera origin). The app feeds the PoG through calibration
 * (which stands in for the model's per-user SVR/affine personalisation).
 */
object OpenGazeGeometry {
    const val PATCH = 128
    private const val MARGIN = 0.4f // extra half-width around the eye, as a fraction of eye size

    // Eye-corner landmark indices: left eye (inner 362, outer 263), right eye (outer 33, inner 133).
    val LEFT_CORNERS = intArrayOf(362, 263)
    val RIGHT_CORNERS = intArrayOf(33, 133)

    /**
     * The 8 eye-corner coordinates `[lx1,ly1,lx2,ly2, rx1,ry1,rx2,ry2]` from the MediaPipe normalised
     * landmark arrays (already in `[0,1]`, matching gaze-track's `/w,/h`). Null if an index is out of
     * range or NaN.
     */
    fun eyeCornerLms(xs: FloatArray, ys: FloatArray): FloatArray? {
        for (i in intArrayOf(362, 263, 33, 133)) {
            if (i >= xs.size || i >= ys.size || xs[i].isNaN() || ys[i].isNaN()) return null
        }
        return floatArrayOf(
            xs[362], ys[362], xs[263], ys[263],
            xs[33], ys[33], xs[133], ys[133],
        )
    }

    /**
     * A square-ish pixel crop box around an eye: the bounding box of [indices], expanded by [MARGIN]
     * and squared to the larger side, centred and clamped to the image. Null if an index is out of
     * range / NaN or the image is empty.
     */
    fun eyeCropSquare(xs: FloatArray, ys: FloatArray, indices: IntArray, imgW: Int, imgH: Int): CropBox? {
        if (imgW <= 0 || imgH <= 0 || indices.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in indices) {
            if (i < 0 || i >= xs.size || i >= ys.size) return null
            val x = xs[i]
            val y = ys[i]
            if (x.isNaN() || y.isNaN()) return null
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val cx = (minX + maxX) * 0.5f * imgW
        val cy = (minY + maxY) * 0.5f * imgH
        val wpx = (maxX - minX) * imgW
        val hpx = (maxY - minY) * imgH
        val half = (maxOf(wpx, hpx) * (0.5f + MARGIN)).coerceAtLeast(1f)
        val left = (cx - half).toInt().coerceIn(0, imgW - 1)
        val top = (cy - half).toInt().coerceIn(0, imgH - 1)
        val right = (cx + half).toInt().coerceIn(left + 1, imgW)
        val bottom = (cy + half).toInt().coerceIn(top + 1, imgH)
        return CropBox(left, top, right - left, bottom - top)
    }
}
