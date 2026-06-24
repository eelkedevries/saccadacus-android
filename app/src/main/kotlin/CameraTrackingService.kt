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
 * Proof-of-feasibility foreground service (prompt 002).
 *
 * Owns the front camera via CameraX [ImageAnalysis] and, for each analysed frame,
 * appends `frameIndex, cameraSensorTimestamp, elapsedRealtimeNanos` to a CSV file in
 * the app's external files dir. No tracking, no ML — this exists only to test whether
 * the OS keeps delivering front-camera frames after the user leaves the app.
 */
class CameraTrackingService : LifecycleService() {

    private lateinit var analysisExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var logWriter: BufferedWriter? = null
    private var frameIndex = 0L

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
                logFrame(frameIndex++, image.imageInfo.timestamp, SystemClock.elapsedRealtimeNanos())
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

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Saccadacus tracking active")
            .setContentText("Recording camera frame timing. Tap Stop to end.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
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
        private const val TAG = "SaccadacusFGS"
    }
}
