package com.example.saccadacusandroid

/**
 * Pure (Android-free, unit-testable) crop geometry for the full-face gaze profile (prompt 051): the
 * square face bounding box over the MediaPipe normalised landmarks, expanded by a small margin and
 * clamped to the image. Serves UniGaze-B and other full-face → pitch/yaw models. Pixel sampling lives
 * in the Android [FaceCropPreprocessor].
 */
object FaceCropGeometry {
    const val PATCH = 224
    private const val MARGIN = 0.1f // extra half-size around the face bbox, as a fraction of its size

    /** Square-ish face crop box over all non-NaN landmarks, expanded by [MARGIN], clamped. Null if empty. */
    fun faceCropBox(xs: FloatArray, ys: FloatArray, imgW: Int, imgH: Int): CropBox? {
        if (imgW <= 0 || imgH <= 0 || xs.isEmpty() || xs.size != ys.size) return null
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var any = false
        for (i in xs.indices) {
            val x = xs[i]
            val y = ys[i]
            if (x.isNaN() || y.isNaN()) continue
            any = true
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (!any) return null
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
