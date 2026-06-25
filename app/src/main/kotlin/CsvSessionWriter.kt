package com.example.saccadacusandroid

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.util.Locale

/**
 * Incremental writer for the combined session CSV (prompt 008; schema per spec
 * §Data schemas). Time-series rows are appended during the session (so a crash leaves a
 * readable `.tmp` partial); event rows are appended at stop; the file is finalised
 * atomically (temp -> rename). All numbers use `Locale.ROOT` ('.' decimals).
 */
class CsvSessionWriter(private val dir: File) {

    private var writer: BufferedWriter? = null
    private var tmpFile: File? = null
    private var finalFile: File? = null
    private var trackingMode = "iris"
    private var eyeMode = "binocular"
    private var wallAnchorMs = 0L

    fun start(trackingMode: String, eyeMode: String, wallAnchorMs: Long, stamp: Long, name: String) {
        this.trackingMode = trackingMode
        this.eyeMode = eyeMode
        this.wallAnchorMs = wallAnchorMs
        val safe = safeName(name)
        val base = if (safe.isEmpty()) "session_$stamp" else "session_${stamp}_$safe"
        finalFile = File(dir, "$base.csv")
        tmpFile = File(dir, "$base.csv.tmp")
        try {
            writer = tmpFile!!.bufferedWriter().apply {
                write(HEADER.joinToString(","))
                newLine()
                flush()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "csv start failed", t)
            writer = null
        }
    }

    @Synchronized
    fun appendSample(
        frame: TrackingFrameResult,
        tElapsedNanos: Long,
        cameraSensorTs: Long,
        cameraTsSource: String,
        gazeScreenX: Float? = null,
        gazeScreenY: Float? = null,
    ) {
        val w = writer ?: return
        val l = frame.leftEye
        val r = frame.rightEye
        val row = emptyRow()
        row[0] = tElapsedNanos.toString()
        row[1] = cameraSensorTs.toString()
        row[2] = cameraTsSource
        row[3] = wallAnchorMs.toString()
        row[4] = "time_series"
        row[5] = trackingMode
        row[6] = eyeMode
        row[7] = f(l?.irisXLocal); row[8] = f(l?.irisYLocal)
        row[9] = f(r?.irisXLocal); row[10] = f(r?.irisYLocal)
        row[13] = f(l?.reliability); row[14] = f(r?.reliability)
        row[17] = f(frame.headPose?.yawDeg); row[18] = f(frame.headPose?.pitchDeg); row[19] = f(frame.headPose?.rollDeg)
        row[23] = "${l?.blinkState ?: ""}|${r?.blinkState ?: ""}"
        row[34] = f(gazeScreenX); row[35] = f(gazeScreenY)
        writeRow(w, row)
    }

    @Synchronized
    fun appendSaccade(event: SaccadeEvent) {
        val w = writer ?: return
        val row = emptyRow()
        row[0] = (event.onsetMs * 1_000_000).toString()
        row[4] = "event"; row[5] = trackingMode; row[6] = eyeMode
        row[24] = "saccade"; row[25] = event.onsetMs.toString(); row[26] = event.offsetMs.toString()
        row[27] = event.durationMs.toString(); row[29] = f(event.amplitude); row[30] = f(event.confidence)
        row[31] = event.headMotionLabel
        writeRow(w, row)
    }

    @Synchronized
    fun appendBlink(event: BlinkEvent) {
        val w = writer ?: return
        val row = emptyRow()
        row[0] = (event.onsetMs * 1_000_000).toString()
        row[4] = "event"; row[5] = trackingMode; row[6] = eyeMode
        row[24] = "blink"; row[25] = event.onsetMs.toString(); row[26] = event.offsetMs.toString()
        row[27] = event.durationMs.toString(); row[30] = f(event.confidence.toDouble())
        writeRow(w, row)
    }

    /** Append a fixation as an `event` row with event_type=fixation (prompt 019); centroid in annotation. */
    @Synchronized
    fun appendFixation(event: FixationEvent) {
        val w = writer ?: return
        val row = emptyRow()
        row[0] = (event.onsetMs * 1_000_000).toString()
        row[4] = "event"; row[5] = trackingMode; row[6] = eyeMode
        row[24] = "fixation"; row[25] = event.onsetMs.toString(); row[26] = event.offsetMs.toString()
        row[27] = event.durationMs.toString(); row[30] = f(event.confidence)
        row[33] = "centroid=${f(event.centroidX)};${f(event.centroidY)}"
        writeRow(w, row)
    }

    /** Append a user interaction marker as a `task` row (prompt 012); columns per the schema. */
    @Synchronized
    fun appendTask(label: String, note: String, tElapsedNanos: Long) {
        val w = writer ?: return
        val row = emptyRow()
        row[0] = tElapsedNanos.toString()
        row[3] = wallAnchorMs.toString()
        row[4] = "task"; row[5] = trackingMode; row[6] = eyeMode
        row[32] = csv(label); row[33] = csv(note)
        writeRow(w, row)
    }

    /** Flush, close, and atomically rename temp -> final. Returns the readable file. */
    @Synchronized
    fun finalizeSession(): File? {
        val w = writer ?: return finalFile ?: tmpFile
        try {
            w.flush()
            w.close()
        } catch (t: Throwable) {
            Log.e(TAG, "csv close failed", t)
        }
        writer = null
        val tmp = tmpFile
        val fin = finalFile
        return if (tmp != null && fin != null && tmp.renameTo(fin)) fin else tmp
    }

    private fun writeRow(w: BufferedWriter, row: List<String>) {
        try {
            w.write(row.joinToString(","))
            w.newLine()
            w.flush()
        } catch (t: Throwable) {
            Log.e(TAG, "csv write failed", t)
        }
    }

    private fun emptyRow() = MutableList(HEADER.size) { "" }

    private fun f(v: Float?): String = if (v == null || v.isNaN()) "" else String.format(Locale.ROOT, "%.5f", v)

    private fun f(v: Double): String = String.format(Locale.ROOT, "%.5f", v)

    /** Keep free text on one CSV cell: no commas or newlines. */
    private fun csv(s: String): String = s.replace(',', ';').replace('\n', ' ').replace('\r', ' ')

    /** Filesystem-safe session-name fragment for the filename: alphanumerics/dash only, capped. */
    private fun safeName(name: String): String =
        name.trim().replace(Regex("[^A-Za-z0-9-]+"), "_").trim('_').take(40)

    companion object {
        private const val TAG = "CsvSessionWriter"
        val HEADER = listOf(
            "elapsed_realtime_nanos", "camera_sensor_timestamp", "camera_timestamp_source", "wallclock_anchor_ms",
            "row_type", "tracking_mode", "eye_selection_mode",
            "left_eye_x_local", "left_eye_y_local", "right_eye_x_local", "right_eye_y_local",
            "binocular_x_local", "binocular_y_local",
            "left_eye_reliability", "right_eye_reliability", "iris_reliability", "pupil_reliability",
            "head_yaw", "head_pitch", "head_roll", "head_translation_x", "head_translation_y", "head_translation_z",
            "blink_state",
            "event_type", "event_onset", "event_offset", "event_duration", "event_direction",
            "event_relative_amplitude", "event_confidence", "event_head_motion_label",
            "task_marker", "annotation",
            "gaze_screen_x", "gaze_screen_y",
        )
    }
}
