package com.example.saccadacusandroid

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix

/**
 * Builds the WebEyeTrack `image` input (prompt 049): a `128×512×3` RGB both-eyes strip in `[0,1]`,
 * reproducing WebEyeTrack's two-stage homography warp — map the radial-padded eye-region quad to a
 * `512×512` square, crop the vertical strip between the warped `STRIP_TOP`/`STRIP_BOTTOM` landmarks,
 * then resize to `512×128`. Android-only (real `Bitmap`/`Matrix`); the quad geometry lives in the pure
 * [WebEyeTrackGeometry]. Returns a flat row-major `H×W×C` (`128×512×3`) buffer, or null if it cannot
 * build the patch.
 */
object WebEyeTrackPreprocessor {
    private const val SQUARE = 512
    private const val OUT_W = 512
    private const val OUT_H = 128

    fun eyeStrip(bitmap: Bitmap, xs: FloatArray, ys: FloatArray): FloatArray? {
        val quad = WebEyeTrackGeometry.eyeStripQuad(xs, ys, bitmap.width, bitmap.height) ?: return null
        val st = WebEyeTrackGeometry.STRIP_TOP
        val sb = WebEyeTrackGeometry.STRIP_BOTTOM
        if (st >= xs.size || sb >= ys.size) return null

        // Stage 1: warp the eye-region quad (L_TOP, L_BOTTOM, R_BOTTOM, R_TOP) to a full SQUARE.
        val dst = floatArrayOf(
            0f, 0f,
            0f, SQUARE.toFloat(),
            SQUARE.toFloat(), SQUARE.toFloat(),
            SQUARE.toFloat(), 0f,
        )
        val m = Matrix()
        if (!m.setPolyToPoly(quad, 0, dst, 0, 4)) return null
        val square = try {
            Bitmap.createBitmap(SQUARE, SQUARE, Bitmap.Config.ARGB_8888)
        } catch (t: Throwable) {
            return null
        }
        Canvas(square).drawBitmap(bitmap, m, null)

        // Strip band: warp landmarks STRIP_TOP/STRIP_BOTTOM by the same matrix to find the row span.
        val pts = floatArrayOf(
            xs[st] * bitmap.width, ys[st] * bitmap.height,
            xs[sb] * bitmap.width, ys[sb] * bitmap.height,
        )
        m.mapPoints(pts)
        val top = pts[1].toInt().coerceIn(0, SQUARE - 1)
        val bottom = pts[3].toInt().coerceIn(top + 1, SQUARE)

        // Stage 2: crop the strip (full width) and resize to OUT_W×OUT_H.
        val strip = try {
            Bitmap.createBitmap(square, 0, top, SQUARE, bottom - top)
        } catch (t: Throwable) {
            square.recycle()
            return null
        }
        square.recycle()
        val scaled = Bitmap.createScaledBitmap(strip, OUT_W, OUT_H, true)
        if (scaled != strip) strip.recycle()

        val n = OUT_W * OUT_H
        val px = IntArray(n)
        scaled.getPixels(px, 0, OUT_W, 0, 0, OUT_W, OUT_H)
        scaled.recycle()

        val out = FloatArray(n * 3)
        var o = 0
        for (i in 0 until n) {
            val c = px[i]
            out[o++] = Color.red(c) / 255f
            out[o++] = Color.green(c) / 255f
            out[o++] = Color.blue(c) / 255f
        }
        return out
    }
}
