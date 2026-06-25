package com.example.saccadacusandroid

/**
 * Pure (Android-free, unit-testable) image ops for the gaze-CNN eye patches (prompt 047).
 *
 * Histogram equalisation matches the contrast-normalisation step of the MPIIGaze appearance-based
 * pipeline (OpenCV `cv2.equalizeHist` on the grayscale eye crop) and doubles as a low-light contrast
 * lift. Kept pure so the pixel sampling (Android `Bitmap`) stays in [GazePreprocessor] while the maths
 * is unit-tested here.
 */
object GazeImageOps {
    /**
     * Histogram-equalise an 8-bit grayscale buffer (values 0..255), matching OpenCV's
     * `cv2.equalizeHist`: build the 256-bin cumulative histogram and remap each pixel
     * `v -> round((cdf[v] - cdfMin) / (N - cdfMin) * 255)`, where `cdfMin` is the first non-zero
     * cumulative count. A flat image (a single intensity) has no spread to equalise and is returned
     * unchanged. Inputs are clamped into `0..255`.
     */
    fun equalizeHist(gray: IntArray): IntArray {
        val n = gray.size
        if (n == 0) return IntArray(0)

        val hist = IntArray(256)
        for (v in gray) hist[v.coerceIn(0, 255)]++

        val cdf = IntArray(256)
        var acc = 0
        for (i in 0 until 256) {
            acc += hist[i]
            cdf[i] = acc
        }

        var cdfMin = 0
        for (i in 0 until 256) {
            if (cdf[i] != 0) {
                cdfMin = cdf[i]
                break
            }
        }

        val denom = n - cdfMin
        if (denom <= 0) return gray.copyOf() // flat image: nothing to equalise

        val lut = IntArray(256)
        for (i in 0 until 256) {
            lut[i] = Math.round((cdf[i] - cdfMin).toFloat() / denom * 255f).coerceIn(0, 255)
        }

        val out = IntArray(n)
        for (i in 0 until n) out[i] = lut[gray[i].coerceIn(0, 255)]
        return out
    }
}
