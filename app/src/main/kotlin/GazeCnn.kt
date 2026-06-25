package com.example.saccadacusandroid

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

/**
 * On-device gaze CNN runtime (prompt 040; multiple side-loaded models, prompt 044). The user places
 * one or more LiteRT/TFLite gaze models in the `gaze_models/` dir of the app's external files dir
 * (the legacy single `gaze_model.tflite` in the root is still honoured) — models are never committed
 * to the repo (no gaze model has redistributable weights; see `docs/gaze_cnn.md`). The selected
 * model is loaded for a session and its name labels the recording so models can be compared. When no
 * model is present this is a no-op and [isAvailable] is false, so the default tracking is unaffected.
 *
 * Model contract: input `[1, 36, 60, 1]` float32 histogram-equalised grayscale in [0,1] (a normalised
 * single-eye patch; prompt 047); output `[1, 2]` float32 = `(pitch, yaw)` in radians.
 */
object GazeCnn {
    const val MODELS_DIR = "gaze_models"
    const val LEGACY_MODEL_FILE = "gaze_model.tflite" // single-file location (prompt 040), still honoured
    private const val NUM_THREADS = 2

    @Volatile private var interpreter: Interpreter? = null

    /** Filename of the currently-loaded model, for labelling recordings; empty when none is loaded. */
    @Volatile
    var activeModel: String = ""
        private set

    /** True only when a side-loaded model has been loaded successfully. */
    val isAvailable: Boolean get() = interpreter != null

    /** Side-loaded model filenames found in `gaze_models/` (plus the legacy single file), sorted. */
    fun availableModels(context: Context): List<String> {
        val dir = context.getExternalFilesDir(null) ?: return emptyList()
        val names = sortedSetOf<String>()
        File(dir, MODELS_DIR).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".tflite") }
            ?.forEach { names.add(it.name) }
        if (File(dir, LEGACY_MODEL_FILE).exists()) names.add(LEGACY_MODEL_FILE)
        return names.toList()
    }

    /**
     * Load the [name]d model (from `gaze_models/`, or the legacy root file), falling back to the first
     * available when [name] is empty or missing. Never throws — absence/failure leaves it unavailable.
     */
    @Synchronized
    fun load(context: Context, name: String) {
        close()
        val dir = context.getExternalFilesDir(null) ?: return
        val models = availableModels(context)
        val chosen = if (name.isNotEmpty() && name in models) name else models.firstOrNull() ?: return
        val file = if (chosen == LEGACY_MODEL_FILE) File(dir, LEGACY_MODEL_FILE) else File(File(dir, MODELS_DIR), chosen)
        if (!file.exists()) return
        interpreter = try {
            Interpreter(mapFile(file), Interpreter.Options().apply { numThreads = NUM_THREADS }) // CPU / XNNPACK
        } catch (t: Throwable) {
            null
        }
        activeModel = if (interpreter != null) chosen else ""
    }

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
        activeModel = ""
    }

    /**
     * Run the model on a `[1,36,60,1]` eye patch (prompt 042); returns `(pitch, yaw)` radians, or
     * null when no model is loaded or inference fails. Called from the single analysis thread.
     */
    fun infer(eyePatch: FloatArray): Pair<Float, Float>? {
        val itp = interpreter ?: return null
        return try {
            val buf = ByteBuffer.allocateDirect(eyePatch.size * 4).order(ByteOrder.nativeOrder())
            for (v in eyePatch) buf.putFloat(v)
            buf.rewind()
            val out = Array(1) { FloatArray(2) }
            itp.run(buf, out)
            Pair(out[0][0], out[0][1])
        } catch (t: Throwable) {
            null
        }
    }

    private fun mapFile(file: File): MappedByteBuffer =
        FileInputStream(file).channel.use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
}
