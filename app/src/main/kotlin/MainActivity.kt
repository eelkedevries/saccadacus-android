package com.example.saccadacusandroid

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.saccadacusandroid.ui.theme.AppTheme
import java.io.File
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ControlScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snapshot by TrackingStats.state.collectAsState()
    val benchmark by BenchmarkStats.state.collectAsState()
    val signals by SignalStats.state.collectAsState()
    val events by EventStats.state.collectAsState()
    val session by SessionStats.state.collectAsState()
    var selectedProfile by remember { mutableStateOf(ProbeConfig.selected) }
    var useCase by remember { mutableStateOf(SessionConfig.useCaseMode) }
    var eyeMode by remember { mutableStateOf(SessionConfig.eyeMode) }
    var rawVideo by remember { mutableStateOf(SessionConfig.rawVideoEnabled) }

    // ~0.5 s tick so "since last frame" keeps climbing even when no frames arrive.
    var nowNanos by remember { mutableStateOf(SystemClock.elapsedRealtimeNanos()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowNanos = SystemClock.elapsedRealtimeNanos()
            delay(500)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val cameraGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            startTrackingService(context)
        }
    }

    val running = snapshot.active
    val paused = snapshot.paused
    val runningSecs = if (snapshot.startElapsedRealtimeNanos > 0L) {
        (maxOf(nowNanos, snapshot.lastFrameElapsedRealtimeNanos) - snapshot.startElapsedRealtimeNanos) / 1_000_000_000.0
    } else {
        0.0
    }
    val sinceLastSecs = if (snapshot.lastFrameElapsedRealtimeNanos > 0L) {
        ((nowNanos - snapshot.lastFrameElapsedRealtimeNanos) / 1_000_000_000.0).coerceAtLeast(0.0)
    } else {
        0.0
    }
    val fps = if (runningSecs > 0.5) snapshot.frameCount / runningSecs else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Saccadacus — camera feasibility probe")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProbeConfig.profiles.forEach { profileOption ->
                Button(
                    enabled = !running && selectedProfile != profileOption,
                    onClick = {
                        selectedProfile = profileOption
                        ProbeConfig.selected = profileOption
                    },
                ) { Text(profileOption.name) }
            }
        }
        Text("Profile: ${selectedProfile.name}")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionConfig.useCaseModes.forEach { mode ->
                Button(
                    enabled = !running && useCase != mode,
                    onClick = { useCase = mode; SessionConfig.useCaseMode = mode },
                ) { Text(mode) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionConfig.eyeModes.forEach { mode ->
                Button(
                    enabled = !running && eyeMode != mode,
                    onClick = { eyeMode = mode; SessionConfig.eyeMode = mode },
                ) { Text(mode) }
            }
        }
        Text("Mode: $useCase · eyes $eyeMode")
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !running,
            onClick = { rawVideo = !rawVideo; SessionConfig.rawVideoEnabled = rawVideo },
        ) { Text(if (rawVideo) "Raw video: ON" else "Raw video: OFF (tap to enable)") }
        if (rawVideo) {
            Text("Raw video will be saved locally this session — you consent.")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !running,
            onClick = {
                val permissions = buildList {
                    add(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissions.toTypedArray())
            },
        ) {
            Text("Start tracking")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = running,
            onClick = { stopTrackingService(context) },
        ) {
            Text("Stop")
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = running && !paused,
                onClick = { pauseTrackingService(context) },
            ) { Text("Pause") }
            Button(
                enabled = running && paused,
                onClick = { resumeTrackingService(context) },
            ) { Text("Resume") }
            Button(
                enabled = running,
                onClick = { markTrackingService(context) },
            ) { Text("Mark") }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !running,
            onClick = { saveLatestSessionToDownloads(context) },
        ) {
            Text("Save CSV to Downloads")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !running,
            onClick = { shareLatestSession(context) },
        ) {
            Text("Share session CSV")
        }

        Spacer(Modifier.height(24.dp))
        Text("Frames logged: ${snapshot.frameCount}")
        Text("Running: ${formatDuration(runningSecs)}")
        Text("Since last frame: ${formatSeconds(sinceLastSecs)} s")
        Text("Approx rate: ${fps.toInt()} fps")
        Text("Face: " + if (snapshot.faceDetected) "detected (${snapshot.landmarkCount} landmarks)" else "not detected")
        Text("Blink L/R: ${"%.2f".format(snapshot.blinkLeft)} / ${"%.2f".format(snapshot.blinkRight)}")
        Spacer(Modifier.height(12.dp))
        Text("Benchmark — analysed ${benchmark.analysedFps.toInt()} fps, dropped ${benchmark.droppedFrames}")
        Text("Inference ms: mean ${benchmark.latencyMeanMs.toInt()}, p50 ${benchmark.latencyP50Ms.toInt()}, p95 ${benchmark.latencyP95Ms.toInt()}")
        signals?.let { s ->
            Spacer(Modifier.height(8.dp))
            Text("L eye-local x/y: ${fmt(s.leftEye?.irisXLocal)} / ${fmt(s.leftEye?.irisYLocal)}")
            Text("R eye-local x/y: ${fmt(s.rightEye?.irisXLocal)} / ${fmt(s.rightEye?.irisYLocal)}")
            Text("Head yaw/pitch/roll: ${headPoseText(s.headPose)}")
        }
        Text("Saccades ${events.saccades} · Blinks ${events.blinks} · ${events.headMotionLabel}")
        Text("Session: ${session.sampleCount} samples · ${session.sensorSampleCount} sensor · ${session.lossIntervalCount} loss · ${session.markerCount} marks · sensors ${if (session.sensorsActive) "on" else "off"}")
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                running && paused -> "Tracking quality: PAUSED"
                running && snapshot.faceDetected -> "Tracking quality: OK"
                running -> "Tracking quality: FACE LOST"
                else -> "Tracking quality: idle"
            },
        )
        Text("Reliability L/R: ${fmt(signals?.leftEye?.reliability)} / ${fmt(signals?.rightEye?.reliability)}")
        if (running && rawVideo) {
            Text("● Recording raw video")
        }
        Spacer(Modifier.height(16.dp))
        Text(verdict(running, sinceLastSecs))
    }
}

private fun verdict(running: Boolean, sinceLastSecs: Double): String = when {
    !running ->
        "Idle. Press Start and grant the camera, then switch to another app or lock the screen for a minute."
    sinceLastSecs < 3.0 ->
        "Frames are arriving. Switch away or lock the screen, then come back: if this count kept rising, the background camera works."
    else ->
        "No frame for ${sinceLastSecs.toInt()} s — the camera was likely blocked or the service was killed."
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

private fun formatSeconds(seconds: Double): String = "%.1f".format(seconds)

private fun fmt(value: Float?): String = if (value == null) "-" else "%.3f".format(value)

private fun headPoseText(pose: HeadPose?): String =
    if (pose == null) "-" else "${pose.yawDeg.toInt()} / ${pose.pitchDeg.toInt()} / ${pose.rollDeg.toInt()}"

private fun startTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_START)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_STOP)
    context.startService(intent)
}

private fun pauseTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_PAUSE)
    context.startService(intent)
}

private fun resumeTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_RESUME)
    context.startService(intent)
}

private fun markTrackingService(context: Context) {
    val intent = Intent(context, CameraTrackingService::class.java)
        .setAction(CameraTrackingService.ACTION_MARK)
    context.startService(intent)
}

private fun latestSessionCsv(context: Context): File? {
    val dir = context.getExternalFilesDir(null) ?: return null
    return dir.listFiles()
        ?.filter { it.name.startsWith("session_") && it.name.endsWith(".csv") }
        ?.maxByOrNull { it.lastModified() }
}

private fun shareLatestSession(context: Context) {
    val csv = latestSessionCsv(context) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csv)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(send, "Share session CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Copy the most recent session CSV into the device's public Downloads collection via
 * MediaStore, so it lands in Files → Downloads with no share-sheet and no third-party
 * app. The Downloads collection is writable without a storage permission on API 29+.
 */
private fun saveLatestSessionToDownloads(context: Context) {
    val csv = latestSessionCsv(context) ?: run {
        Toast.makeText(context, "No session CSV yet — record and stop a session first.", Toast.LENGTH_LONG).show()
        return
    }
    val resolver = context.contentResolver
    val pending = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, csv.name)
        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = resolver.insert(collection, pending) ?: run {
        Toast.makeText(context, "Could not save to Downloads.", Toast.LENGTH_LONG).show()
        return
    }
    try {
        resolver.openOutputStream(uri)?.use { output ->
            csv.inputStream().use { input -> input.copyTo(output) }
        } ?: error("could not open Downloads file for writing")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        Toast.makeText(context, "Saved to Downloads: ${csv.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
