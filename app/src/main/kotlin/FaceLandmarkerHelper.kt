package com.example.saccadacusandroid

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
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

    /** Feed one frame. [timestampMs] must be strictly increasing across calls. */
    fun detectAsync(bitmap: Bitmap, rotationDegrees: Int, timestampMs: Long) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val imageOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()
        faceLandmarker?.detectAsync(mpImage, imageOptions, timestampMs)
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
