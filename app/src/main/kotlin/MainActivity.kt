package com.example.saccadacusandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.saccadacusandroid.ui.ChipSelector
import com.example.saccadacusandroid.ui.LabeledSwitch
import com.example.saccadacusandroid.ui.SectionCard
import com.example.saccadacusandroid.ui.StatRow
import com.example.saccadacusandroid.ui.theme.AppTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)
        recoverOrphanedSessions(this)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppRoot()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AppSettings.save(this) // persist whatever the user changed when the app is backgrounded
    }
}

private enum class Screen { CONTROL, SESSIONS, CALIBRATION }

/**
 * Top-level routing + the Material-3 top app bar (prompt 053). Onboarding and the full-screen
 * calibration render outside the [Scaffold] (calibration needs the whole screen); Control and Sessions
 * render inside it under the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    var onboardingDone by remember { mutableStateOf(AppSettings.isFirstRunDone(context)) }
    if (!onboardingDone) {
        OnboardingScreen(onDone = {
            AppSettings.setFirstRunDone(context)
            onboardingDone = true
        })
        return
    }
    var screen by remember { mutableStateOf(Screen.CONTROL) }
    if (screen == Screen.CALIBRATION) {
        CalibrationScreen(onBack = { screen = Screen.CONTROL })
        return
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (screen == Screen.SESSIONS) {
                TopAppBar(
                    title = { Text("Sessions") },
                    navigationIcon = {
                        TextButton(onClick = { screen = Screen.CONTROL }) { Text("Back") }
                    },
                )
            } else {
                TopAppBar(title = { Text("Saccadacus") })
            }
        },
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        if (screen == Screen.SESSIONS) {
            SessionsScreen(modifier = screenModifier)
        } else {
            ControlScreen(
                modifier = screenModifier,
                onOpenSessions = { screen = Screen.SESSIONS },
                onOpenCalibration = { screen = Screen.CALIBRATION },
            )
        }
    }
}

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    onOpenSessions: () -> Unit,
    onOpenCalibration: () -> Unit,
) {
    val context = LocalContext.current
    val snapshot by TrackingStats.state.collectAsState()
    val benchmark by BenchmarkStats.state.collectAsState()
    val signals by SignalStats.state.collectAsState()
    val events by EventStats.state.collectAsState()
    val session by SessionStats.state.collectAsState()
    val overlay by OverlayStats.state.collectAsState()
    val quality by QualityStats.state.collectAsState()
    val summary by SummaryStats.state.collectAsState()
    val gaze by GazeStats.state.collectAsState()
    val calModel by CalibrationStore.state.collectAsState()
    val calError by CalibrationStore.error.collectAsState()
    var selectedProfile by remember { mutableStateOf(ProbeConfig.selected) }
    var useCase by remember { mutableStateOf(SessionConfig.useCaseMode) }
    var eyeMode by remember { mutableStateOf(SessionConfig.eyeMode) }
    var rawVideo by remember { mutableStateOf(SessionConfig.rawVideoEnabled) }
    var overlayEnabled by remember { mutableStateOf(OverlayConfig.enabled) }
    var sessionName by remember { mutableStateOf(SessionConfig.sessionName) }
    var sessionNote by remember { mutableStateOf(SessionConfig.sessionNote) }
    var filterEnabled by remember { mutableStateOf(SessionConfig.filterEnabled) }
    var signalSource by remember { mutableStateOf(SessionConfig.signalSource) }
    var gazeModel by remember { mutableStateOf(SessionConfig.gazeModelName) }
    var showRationale by remember { mutableStateOf(false) }
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    // ~0.5 s tick so "since last frame" keeps climbing even when no frames arrive.
    var nowNanos by remember { mutableStateOf(SystemClock.elapsedRealtimeNanos()) }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var batteryExempt by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
    LaunchedEffect(Unit) {
        while (true) {
            nowNanos = SystemClock.elapsedRealtimeNanos()
            batteryExempt = powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
    val cameraLost = snapshot.cameraLost
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard("Recording") {
            val stateLine = when {
                running && cameraLost -> "Camera lost — retrying…"
                running && paused -> "Paused"
                running && snapshot.faceDetected -> "Recording · face detected"
                running -> "Recording · face not detected"
                else -> "Idle — ready to record"
            }
            Text(
                stateLine,
                style = MaterialTheme.typography.titleSmall,
                color = if (running && !paused && !cameraLost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (running) {
                StatRow("Elapsed", formatDuration(runningSecs))
                StatRow("Rate", "${fps.toInt()} fps")
                StatRow("Since last frame", "${formatSeconds(sinceLastSecs)} s")
                StatRow(
                    "Lighting",
                    when (quality.label) {
                        QualitySnapshot.LOW_LIGHT -> "Low (luma ${quality.luma.toInt()}) — may be unreliable"
                        QualitySnapshot.FACE_LOST -> "No face in view"
                        QualitySnapshot.GOOD -> "Good (luma ${quality.luma.toInt()})"
                        else -> "—"
                    },
                )
                if (rawVideo) StatRow("Raw video", "● recording")
            }
            if (snapshot.resourceWarning.isNotEmpty()) {
                Text(
                    "⚠ ${snapshot.resourceWarning}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!running) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(requiredPermissions)
                        } else {
                            showRationale = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start recording") }
            } else {
                Button(
                    onClick = { stopTrackingService(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Stop recording") }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        enabled = !paused,
                        onClick = { pauseTrackingService(context) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Pause") }
                    OutlinedButton(
                        enabled = paused,
                        onClick = { resumeTrackingService(context) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Resume") }
                    OutlinedButton(
                        onClick = { markTrackingService(context) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Mark") }
                }
                Text(
                    "“Mark” drops a timestamp into the recording (e.g. when a task starts).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionCard(
            "Recording setup",
            if (running) "Locked while recording." else "Set these before you start.",
        ) {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it; SessionConfig.sessionName = it },
                enabled = !running,
                singleLine = true,
                label = { Text("Session name (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sessionNote,
                onValueChange = { sessionNote = it; SessionConfig.sessionNote = it },
                enabled = !running,
                singleLine = true,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            ChipSelector(
                label = "Quality profile",
                description = "Higher quality uses more battery.",
                options = ProbeConfig.profiles,
                selected = selectedProfile,
                enabled = !running,
                labelOf = { it.name },
                onSelect = { selectedProfile = it; ProbeConfig.selected = it },
            )
            ChipSelector(
                label = "Use case",
                options = SessionConfig.useCaseModes,
                selected = useCase,
                enabled = !running,
                onSelect = { useCase = it; SessionConfig.useCaseMode = it },
            )
            ChipSelector(
                label = "Eyes tracked",
                options = SessionConfig.eyeModes,
                selected = eyeMode,
                enabled = !running,
                onSelect = { eyeMode = it; SessionConfig.eyeMode = it },
            )
            ChipSelector(
                label = "Gaze source",
                description = "How gaze is estimated. Iris is the default; CNN runs a side-loaded model.",
                options = listOf(SessionConfig.SOURCE_IRIS, SessionConfig.SOURCE_BLENDSHAPE, SessionConfig.SOURCE_CNN),
                selected = signalSource,
                enabled = !running,
                onSelect = { signalSource = it; SessionConfig.signalSource = it },
            )
            if (signalSource == SessionConfig.SOURCE_CNN) {
                val models = remember(signalSource, running) { GazeCnn.availableModels(context) }
                if (models.isEmpty()) {
                    Text(
                        "No CNN models found. Side-load a .tflite into gaze_models/ (see docs/gaze_cnn.md).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ChipSelector(
                        label = "Model",
                        options = models,
                        selected = gazeModel.ifEmpty { models.first() },
                        enabled = !running,
                        onSelect = {
                            gazeModel = it
                            SessionConfig.gazeModelName = it
                            AppSettings.save(context)
                        },
                    )
                }
            }
            LabeledSwitch(
                label = "Smoothing",
                description = "Reduce jitter in the gaze signal.",
                checked = filterEnabled,
                enabled = !running,
                onCheckedChange = { filterEnabled = it; SessionConfig.filterEnabled = it },
            )
            LabeledSwitch(
                label = "Save raw video",
                description = "Stores the camera video locally this session (off by default).",
                checked = rawVideo,
                enabled = !running,
                onCheckedChange = { rawVideo = it; SessionConfig.rawVideoEnabled = it },
            )
            LabeledSwitch(
                label = "Camera overlay",
                description = "Show the face mesh + iris dots in Details, to check tracking.",
                checked = overlayEnabled,
                onCheckedChange = { overlayEnabled = it; OverlayConfig.enabled = it },
            )
            if (batteryExempt) {
                StatRow("Background", "Unrestricted ✓")
            } else {
                OutlinedButton(
                    onClick = { requestIgnoreBatteryOptimizations(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Allow background recording (battery)") }
            }
        }

        SectionCard("Gaze calibration", "Improves where the gaze dot lands on screen.") {
            StatRow(
                "Status",
                if (calModel != null) {
                    "Calibrated" + (calError?.let { " · check err ${"%.3f".format(it)}" } ?: "")
                } else {
                    "Not calibrated"
                },
            )
            Button(
                onClick = onOpenCalibration,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Calibrate gaze") }
        }

        SectionCard("Saved data", "Export the most recent session, or browse all of them.") {
            Button(
                enabled = !running,
                onClick = { saveLatestSessionToDownloads(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save last session to Downloads") }
            OutlinedButton(
                enabled = !running,
                onClick = { shareLatestSession(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Share last session CSV") }
            OutlinedButton(
                onClick = onOpenSessions,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("All sessions") }
            summary?.let { s ->
                if (!running) {
                    HorizontalDivider()
                    Text("Last session", style = MaterialTheme.typography.labelLarge)
                    StatRow("Duration", "${"%.0f".format(s.durationSec)} s")
                    StatRow("Saccades", "${s.saccades} (${s.saccadeRatePerMin.toInt()}/min)")
                    StatRow("Fixations", "${s.fixations} (${s.fixationRatePerMin.toInt()}/min)")
                    StatRow("Blinks", "${s.blinks} (${s.blinkRatePerMin.toInt()}/min)")
                    StatRow("Mean reliability", "%.2f".format(s.meanReliability))
                    StatRow("Tracking loss", "${"%.1f".format(s.trackingLossSec)} s")
                }
            }
        }

        var detailsExpanded by remember { mutableStateOf(false) }
        SectionCard("Details & diagnostics") {
            OutlinedButton(
                onClick = { detailsExpanded = !detailsExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (detailsExpanded) "Hide details" else "Show details") }
            if (detailsExpanded) {
                StatRow("Frames logged", "${snapshot.frameCount}")
                StatRow("Face", if (snapshot.faceDetected) "detected (${snapshot.landmarkCount} lm)" else "not detected")
                StatRow("Blink L/R", "${"%.2f".format(snapshot.blinkLeft)} / ${"%.2f".format(snapshot.blinkRight)}")
                StatRow("Analysed", "${benchmark.analysedFps.toInt()} fps · dropped ${benchmark.droppedFrames}")
                StatRow(
                    "Inference ms",
                    "mean ${benchmark.latencyMeanMs.toInt()} · p50 ${benchmark.latencyP50Ms.toInt()} · p95 ${benchmark.latencyP95Ms.toInt()}",
                )
                signals?.let { s ->
                    StatRow("L eye-local x/y", "${fmt(s.leftEye?.irisXLocal)} / ${fmt(s.leftEye?.irisYLocal)}")
                    StatRow("R eye-local x/y", "${fmt(s.rightEye?.irisXLocal)} / ${fmt(s.rightEye?.irisYLocal)}")
                    StatRow("Head y/p/r", headPoseText(s.headPose))
                    StatRow("Reliability L/R", "${fmt(s.leftEye?.reliability)} / ${fmt(s.rightEye?.reliability)}")
                }
                StatRow("Events", "sacc ${events.saccades} · fix ${events.fixations} · blink ${events.blinks}")
                StatRow("Head motion", events.headMotionLabel)
                StatRow(
                    "Session",
                    "${session.sampleCount} samp · ${session.sensorSampleCount} sens · ${session.lossIntervalCount} loss · ${session.markerCount} marks${if (session.sensorsActive) " · sensors on" else " · sensors off"}",
                )
                Text(
                    verdict(running, sinceLastSecs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (overlayEnabled) {
                    Text(
                        "Overlay — green = mesh, red = iris, yellow = gaze. Look around; red dots should follow your eyes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.Black),
                    ) {
                        val of = overlay
                        if (of != null && of.landmarks.size >= 2) {
                            val w = size.width
                            val h = size.height
                            var i = 0
                            while (i + 1 < of.landmarks.size) {
                                // Mirror x to a selfie view (matches SignConvention); y is image-down.
                                drawCircle(Color.Green, radius = 2f, center = Offset((1f - of.landmarks[i]) * w, of.landmarks[i + 1] * h))
                                i += 2
                            }
                            for (idx in intArrayOf(of.leftIrisIndex, of.rightIrisIndex)) {
                                val base = idx * 2
                                if (base + 1 < of.landmarks.size) {
                                    drawCircle(Color.Red, radius = 9f, center = Offset((1f - of.landmarks[base]) * w, of.landmarks[base + 1] * h))
                                }
                            }
                        }
                        // Calibrated point-of-gaze (normalised screen), when calibrated.
                        gaze?.let { (gx, gy) ->
                            drawCircle(Color.Yellow, radius = 14f, center = Offset(gx * size.width, gy * size.height))
                        }
                    }
                }
                var modelsExpanded by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { modelsExpanded = !modelsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (modelsExpanded) "Hide model comparison" else "Compare gaze models") }
                if (modelsExpanded) {
                    Text(
                        "Reference snapshot — weights are research-only; verify upstream.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GazeModels.all.forEachIndexed { i, m ->
                        Text("${i + 1}. ${m.name}", fontWeight = FontWeight.Bold)
                        Text("${m.publisher} · ${m.license} · ${m.year}", style = MaterialTheme.typography.bodySmall)
                        Text("${m.accuracy} · ${m.size}", style = MaterialTheme.typography.bodySmall)
                        Text("+ ${m.pros}", style = MaterialTheme.typography.bodySmall)
                        Text("- ${m.cons}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (showRationale) {
            AlertDialog(
                onDismissRequest = { showRationale = false },
                title = { Text("Camera & notifications") },
                text = {
                    Text(
                        "Saccadacus needs the camera to track your eyes, and the notification " +
                            "permission to show the ongoing recording. Recording only ever runs while " +
                            "you can see the notification. You'll be asked to grant these next.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRationale = false
                        permissionLauncher.launch(requiredPermissions)
                    }) { Text("Continue") }
                },
                dismissButton = {
                    TextButton(onClick = { showRationale = false }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onDone: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome to Saccadacus", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Saccadacus records your eye and head movements from the front camera while you use your " +
                "phone, to study reading and attention.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionCard("How it works") {
            Text(
                "• Recording only starts when you press Start; a notification and the camera indicator stay visible throughout.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "• Everything is processed on your device — nothing is uploaded. Video is never recorded unless you turn it on.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "• It needs the camera permission to see your eyes and the notification permission to show the ongoing recording.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Got it") }
    }
}

@Composable
fun CalibrationScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val snapshot by TrackingStats.state.collectAsState()
    // 3x3 fit grid (>= 6 points for the 2nd-order polynomial fit, prompt 038) + interior held-out checks.
    val fitTargets = remember {
        listOf(
            0.15f to 0.15f, 0.5f to 0.15f, 0.85f to 0.15f,
            0.15f to 0.5f, 0.5f to 0.5f, 0.85f to 0.5f,
            0.15f to 0.85f, 0.5f to 0.85f, 0.85f to 0.85f,
        )
    }
    val valTargets = remember { listOf(0.3f to 0.3f, 0.7f to 0.3f, 0.3f to 0.7f, 0.7f to 0.7f) }
    val allTargets = remember { fitTargets + valTargets }
    val fitCollected = remember { mutableStateListOf<CalibrationSample>() }
    val valCollected = remember { mutableStateListOf<CalibrationSample>() }
    var index by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf("Start tracking, then tap Begin and look at each red dot.") }

    LaunchedEffect(index) {
        if (index in allTargets.indices) {
            val phase = if (index < fitTargets.size) "calibrate" else "check"
            status = "Look at the dot ($phase ${index + 1}/${allTargets.size})"
            delay(800)
            var sumX = 0f
            var sumY = 0f
            var n = 0
            repeat(20) {
                binocularGaze(SignalStats.state.value)?.let { (gx, gy) ->
                    sumX += gx; sumY += gy; n++
                }
                delay(50)
            }
            if (n > 0) {
                val (tx, ty) = allTargets[index]
                val sample = CalibrationSample(sumX / n, sumY / n, tx, ty)
                if (index < fitTargets.size) fitCollected.add(sample) else valCollected.add(sample)
            }
            index += 1
        } else if (index == allTargets.size) {
            val fit = GazeCalibrator.fitBest(fitCollected.toList(), valCollected.toList())
            if (fit != null) {
                val error = fit.error.takeIf { !it.isNaN() }
                CalibrationStore.set(fit.model, error)
                AppSettings.save(context)
                status = "Calibration saved. " +
                    (error?.let { "Mean check error ${"%.3f".format(it)} (screen units)." } ?: "")
            } else {
                status = "Calibration failed — no usable gaze captured. Try again in good light."
            }
            index = -2
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (index in allTargets.indices) {
            Canvas(Modifier.fillMaxSize()) {
                val (tx, ty) = allTargets[index]
                drawCircle(Color.Red, radius = 24f, center = Offset(tx * size.width, ty * size.height))
            }
        }
        Column(modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(16.dp)) {
            Text(status, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = snapshot.active && index < 0,
                    onClick = { fitCollected.clear(); valCollected.clear(); index = 0 },
                ) { Text("Begin") }
                Button(
                    onClick = {
                        CalibrationStore.clear()
                        AppSettings.save(context)
                        status = "Calibration cleared."
                    },
                ) { Text("Clear") }
                Button(onClick = onBack) { Text("Back") }
            }
            if (!snapshot.active) {
                Spacer(Modifier.height(8.dp))
                Text("(Tracking isn't running — start it first, then calibrate.)", color = Color.White)
            }
        }
    }
}

@Composable
fun SessionsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(listSessionCsvs(context)) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${sessions.size} saved session(s)", style = MaterialTheme.typography.titleMedium)
        if (sessions.isEmpty()) {
            Text(
                "No saved sessions yet. Record and stop a session, then it appears here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        sessions.forEach { file ->
            val meta = sessionMeta(file)
            SectionCard(meta["session_name"]?.takeIf { it.isNotBlank() } ?: "(unnamed)") {
                meta["session_note"]?.takeIf { it.isNotBlank() }?.let {
                    Text("note: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "${file.name} · ${file.length() / 1024} kB · ${formatStamp(file.lastModified())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { saveSessionToDownloads(context, file) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                    OutlinedButton(
                        onClick = { shareFile(context, file) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Share") }
                    OutlinedButton(
                        onClick = { pendingDelete = file },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete") }
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete session?") },
            text = { Text("Delete ${toDelete.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete.delete()
                    sessions = listSessionCsvs(context)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
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

/** Open the system battery-optimisation request dialog for this app (013). User-driven, never silent. */
@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(request)
    } catch (t: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (t2: Throwable) {
            Toast.makeText(context, "Battery settings unavailable on this device.", Toast.LENGTH_LONG).show()
        }
    }
}

private fun latestSessionCsv(context: Context): File? {
    val dir = context.getExternalFilesDir(null) ?: return null
    return dir.listFiles()
        ?.filter { it.name.startsWith("session_") && it.name.endsWith(".csv") }
        ?.maxByOrNull { it.lastModified() }
}

/**
 * Recover sessions left as `.tmp` by a killed service (prompt 022): rename each leftover
 * `session_*.csv.tmp` to its `.csv` form so the data is not lost. Skipped entirely while a
 * session is active (the in-progress `.tmp` must not be touched), and never overwrites an
 * existing `.csv`.
 */
private fun recoverOrphanedSessions(context: Context) {
    if (TrackingStats.state.value.active) return
    val dir = context.getExternalFilesDir(null) ?: return
    dir.listFiles()
        ?.filter { it.name.startsWith("session_") && it.name.endsWith(".csv.tmp") }
        ?.forEach { tmp ->
            val finalFile = File(tmp.parentFile, tmp.name.removeSuffix(".tmp"))
            if (!finalFile.exists()) {
                try {
                    tmp.renameTo(finalFile)
                } catch (t: Throwable) {
                    // leave the .tmp in place if recovery fails
                }
            }
        }
}

/** Finalised session CSVs, newest first. The active session writes a `.tmp`, so it is excluded. */
private fun listSessionCsvs(context: Context): List<File> {
    val dir = context.getExternalFilesDir(null) ?: return emptyList()
    return dir.listFiles()
        ?.filter { it.name.startsWith("session_") && it.name.endsWith(".csv") }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}

private fun formatStamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).format(Date(epochMs))

/** Read a session's meta_<stamp>.csv sidecar (prompt 018) into key→value pairs; empty if absent. */
private fun sessionMeta(file: File): Map<String, String> {
    val stamp = file.name.removePrefix("session_").takeWhile { it.isDigit() }
    if (stamp.isEmpty()) return emptyMap()
    val meta = File(file.parentFile, "meta_$stamp.csv")
    if (!meta.exists()) return emptyMap()
    return try {
        meta.readLines().drop(1).mapNotNull { line ->
            val idx = line.indexOf(',')
            if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
        }.toMap()
    } catch (t: Throwable) {
        emptyMap()
    }
}

private fun shareLatestSession(context: Context) {
    val csv = latestSessionCsv(context) ?: return
    shareFile(context, csv)
}

private fun shareFile(context: Context, csv: File) {
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
    saveSessionToDownloads(context, csv)
}

/**
 * All files belonging to a session: the combined CSV plus every sidecar sharing its `<stamp>`
 * (meta / summary / sensors, and benchmark / frame-log / raw video when present). The stamp is a
 * unique millisecond value, so a `contains` match cannot pull in another session's files.
 */
private fun sessionBundle(csv: File): List<File> {
    val stamp = csv.name.removePrefix("session_").takeWhile { it.isDigit() }
    val dir = csv.parentFile
    if (stamp.isEmpty() || dir == null) return listOf(csv)
    val companions = dir.listFiles()
        ?.filter { it.isFile && it != csv && it.name.contains(stamp) }
        ?.sortedBy { it.name }
        ?: emptyList()
    return listOf(csv) + companions
}

/** Copy a session and all its sidecars into public Downloads, with one summary toast. */
private fun saveSessionToDownloads(context: Context, csv: File) {
    val files = sessionBundle(csv)
    val saved = files.count { saveFileToDownloads(context, it) }
    val msg = if (saved == files.size) {
        "Saved $saved file(s) to Downloads (session + sidecars)."
    } else {
        "Saved $saved of ${files.size} file(s) to Downloads."
    }
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

/** Copy a single file into public Downloads; returns true on success. MIME from the extension. */
private fun saveFileToDownloads(context: Context, file: File): Boolean {
    val resolver = context.contentResolver
    val mime = when (file.extension.lowercase(Locale.UK)) {
        "csv" -> "text/csv"
        "mp4" -> "video/mp4"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
    val pending = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
        put(MediaStore.Downloads.MIME_TYPE, mime)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val uri = resolver.insert(collection, pending) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("could not open Downloads file for writing")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        true
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        false
    }
}
