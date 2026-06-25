package com.example.saccadacusandroid

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

/**
 * On-device gaze CNN runtime (prompt 040). Loads a **side-loaded** LiteRT/TFLite gaze model that the
 * user places at [MODEL_FILE] in the app's external files dir — the model is never committed to the
 * repo (no gaze model has redistributable weights; see `docs-dev/.../gaze_cnn_contract.md`). When
 * the file is absent this is a no-op and [isAvailable] is false, so the default iris/blendshape
 * tracking is unaffected.
 *
 * Model contract: input `[1, 36, 60, 1]` float32 grayscale in [0,1] (a normalised single-eye patch);
 * output `[1, 2]` float32 = `(pitch, yaw)` in radians. Inference is added in prompt 042.
 */
object GazeCnn {
    const val MODEL_FILE = "gaze_model.tflite"
    private const val NUM_THREADS = 2

    @Volatile private var interpreter: Interpreter? = null

    /** True only when a side-loaded model has been loaded successfully. */
    val isAvailable: Boolean get() = interpreter != null

    /** Load the side-loaded model if present. Never throws — absence or failure leaves it unavailable. */
    @Synchronized
    fun load(context: Context) {
        close()
        val file = File(context.getExternalFilesDir(null), MODEL_FILE)
        if (!file.exists()) return
        interpreter = try {
            val options = Interpreter.Options().apply { numThreads = NUM_THREADS } // CPU / XNNPACK
            Interpreter(mapFile(file), options)
        } catch (t: Throwable) {
            null
        }
    }

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun mapFile(file: File): MappedByteBuffer =
        FileInputStream(file).channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
}
