package com.example.saccadacusandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Thin wrapper over the MediaPipe Tasks Vision Face Landmarker in LIVE_STREAM mode
 * (prompt 003). Produces 478 landmarks, blendshapes (incl. eye-blink), and the facial
 * transformation matrix. No derived signals here — that is prompt 005.
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (FaceLandmarkerResult) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var faceLandmarker: FaceLandmarker? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(Delegate.CPU)
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)
                .setResultListener { result, _ -> onResult(result) }
                .setErrorListener { error -> onError(error.message ?: "unknown MediaPipe error") }
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (t: Throwable) {
            Log.e(TAG, "FaceLandmarker init failed", t)
            onError(t.message ?: "FaceLandmarker init failed")
        }
    }

    /**
     * Feed one frame. [timestampMs] must be strictly increasing across calls.
     *
     * The frame is rotated upright here (rather than via [ImageProcessingOptions]) because
     * MediaPipe does not apply that rotation hint to the returned landmark coordinates or
     * the facial-transformation matrix — leaving the face sideways (a constant ~90° roll)
     * and corrupting the eye-corner projection. Rotating the bitmap is the approach the
     * official CameraX + MediaPipe sample uses. The front-camera mirror is handled
     * downstream in [SignConvention], so it is deliberately not applied here.
     */
    fun detectAsync(bitmap: Bitmap, rotationDegrees: Int, timestampMs: Long) {
        val upright = if (rotationDegrees % 360 == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        val mpImage = BitmapImageBuilder(upright).build()
        faceLandmarker?.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        try {
            faceLandmarker?.close()
        } catch (t: Throwable) {
            Log.e(TAG, "FaceLandmarker close failed", t)
        }
        faceLandmarker = null
    }

    companion object {
        const val MODEL_ASSET = "face_landmarker.task"
        private const val TAG = "FaceLandmarkerHelper"
    }
}
