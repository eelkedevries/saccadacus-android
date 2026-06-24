package com.example.saccadacusandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import java.io.BufferedWriter
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Proof-of-feasibility foreground service (prompts 002 / 002b).
 *
 * Owns the front camera via CameraX [ImageAnalysis] and, for each analysed frame,
 * appends `frameIndex, cameraSensorTimestamp, elapsedRealtimeNanos` to a CSV file in
 * the app's external files dir, and updates [TrackingStats] + the notification so
 * progress is visible without pulling the CSV. No tracking, no ML — this exists only
 * to test whether the OS keeps delivering front-camera frames after the user leaves.
 */
class CameraTrackingService : LifecycleService() {

    private lateinit var analysisExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var logWriter: BufferedWriter? = null
    private var frameIndex = 0L
    private var lastNotifUpdateMs = 0L

    override fun onCreate() {
        super.onCreate()
        analysisExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopTracking()
        } else {
            startTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        createChannel()
        startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        TrackingStats.onStart(SystemClock.elapsedRealtimeNanos())
        openLog()
        bindCamera()
    }

    private fun bindCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = try {
                future.get()
            } catch (t: Throwable) {
                Log.e(TAG, "camera provider unavailable", t)
                return@addListener
            }
            cameraProvider = provider
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { image ->
                val elapsed = SystemClock.elapsedRealtimeNanos()
                val index = frameIndex++
                logFrame(index, image.imageInfo.timestamp, elapsed)
                TrackingStats.onFrame(elapsed)
                maybeUpdateNotification(index + 1)
                image.close()
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            } catch (t: Throwable) {
                Log.e(TAG, "bindToLifecycle failed", t)
            }
        }, mainExecutor)
    }

    private fun openLog() {
        try {
            val file = File(getExternalFilesDir(null), "frame_log_${System.currentTimeMillis()}.csv")
            logWriter = file.bufferedWriter().apply {
                write("frame_index,camera_sensor_timestamp,elapsed_realtime_nanos")
                newLine()
                flush()
            }
            Log.i(TAG, "Logging frames to: ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "openLog failed", t)
        }
    }

    private fun logFrame(index: Long, sensorTimestamp: Long, elapsedRealtimeNanos: Long) {
        try {
            logWriter?.apply {
                write("$index,$sensorTimestamp,$elapsedRealtimeNanos")
                newLine()
                flush()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "frame log write failed", t)
        }
    }

    /** Refresh the notification with the running frame count, at most ~once every 2 s. */
    private fun maybeUpdateNotification(frameCount: Long) {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastNotifUpdateMs < NOTIF_UPDATE_INTERVAL_MS) return
        lastNotifUpdateMs = nowMs
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIF_ID, buildNotification("Frames logged: $frameCount. Keep running; tap Stop to end."))
        } catch (t: Throwable) {
            Log.e(TAG, "notification update failed", t)
        }
    }

    private fun stopTracking() {
        closeResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        closeResources()
        analysisExecutor.shutdown()
        super.onDestroy()
    }

    private fun closeResources() {
        try {
            cameraProvider?.unbindAll()
        } catch (t: Throwable) {
            Log.e(TAG, "unbind failed", t)
        }
        cameraProvider = null
        try {
            logWriter?.flush()
            logWriter?.close()
        } catch (t: Throwable) {
            Log.e(TAG, "close log failed", t)
        }
        logWriter = null
        TrackingStats.onStop()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Saccadacus tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Active camera tracking session" },
        )
    }

    private fun buildNotification(text: String = "Recording camera frame timing. Tap Stop to end.") =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Saccadacus tracking active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                "Stop",
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, CameraTrackingService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    companion object {
        const val ACTION_START = "com.example.saccadacusandroid.action.START"
        const val ACTION_STOP = "com.example.saccadacusandroid.action.STOP"
        private const val CHANNEL_ID = "saccadacus_tracking"
        private const val NOTIF_ID = 1
        private const val NOTIF_UPDATE_INTERVAL_MS = 2000L
        private const val TAG = "SaccadacusFGS"
    }
}
