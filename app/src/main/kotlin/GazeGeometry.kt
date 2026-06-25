package com.example.saccadacusandroid

/** A pixel crop rectangle in an image. */
data class CropBox(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * Pure (Android-free, unit-testable) crop geometry for the gaze CNN eye patches (prompt 041).
 * From the MediaPipe normalised landmark arrays it computes each eye's crop box: the bounding box of
 * a few eye-ring landmarks, expanded by a margin, shaped to the patch aspect (60×36), centred on the
 * eye, and clamped to the image. The pixel sampling itself lives in the Android-only GazePreprocessor.
 */
object GazeGeometry {
    const val PATCH_W = 60
    const val PATCH_H = 36
    private const val MARGIN = 0.6f // extra half-width added around the eye, as a fraction of eye width

    // Representative eye-ring landmark indices (outer/inner corner + upper/lower lids) per eye.
    val RIGHT_EYE = intArrayOf(33, 133, 159, 145, 158, 153)
    val LEFT_EYE = intArrayOf(362, 263, 386, 374, 385, 380)

    /**
     * Crop box (pixels) for one eye from normalised landmark x/y arrays + image size; null if any
     * index is out of range or NaN, or the image is empty.
     */
    fun eyeCrop(xs: FloatArray, ys: FloatArray, indices: IntArray, imgW: Int, imgH: Int): CropBox? {
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
        val halfW = ((maxX - minX) * imgW * (0.5f + MARGIN)).coerceAtLeast(1f)
        val halfH = halfW * (PATCH_H.toFloat() / PATCH_W.toFloat())
        val left = (cx - halfW).toInt().coerceIn(0, imgW - 1)
        val top = (cy - halfH).toInt().coerceIn(0, imgH - 1)
        val right = (cx + halfW).toInt().coerceIn(left + 1, imgW)
        val bottom = (cy + halfH).toInt().coerceIn(top + 1, imgH)
        return CropBox(left, top, right - left, bottom - top)
    }
}
