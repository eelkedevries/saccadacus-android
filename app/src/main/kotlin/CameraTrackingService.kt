package com.example.saccadacusandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.BufferedWriter
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Foreground service that owns the front camera (002 / 002b), the MediaPipe Face
 * Landmarker (003), and the on-device benchmark (004). Per frame it logs frame timing,
 * (per the active profile cadence) feeds MediaPipe, and times inference; face/landmark
 * info is published via [TrackingStats] and benchmark stats via [BenchmarkStats].
 */
class CameraTrackingService : LifecycleService() {

    private lateinit var analysisExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var faceHelper: FaceLandmarkerHelper? = null

    // Orientation robustness (023): keep the analysis target rotation following the device so
    // imageInfo.rotationDegrees (used to rotate the frame upright) stays correct on rotation.
    private var imageAnalysis: ImageAnalysis? = null
    private var orientationListener: OrientationEventListener? = null
    private var logWriter: BufferedWriter? = null
    private var frameIndex = 0L
    private var lastNotifUpdateMs = 0L
    private var lastMpTimestamp = 0L

    // Pause/resume (011): when paused the camera stays bound (indicator + notification
    // persist) but frames are dropped before logging/inference/recording.
    @Volatile private var paused = false

    // Benchmark (004)
    private lateinit var profile: TrackingProfile
    private val submitTimesNanos = ConcurrentHashMap<Long, Long>()
    private val latenciesMs = Collections.synchronizedList(ArrayList<Double>())

    // CNN gaze inference latency, ms (043); populated only while the CNN source runs.
    private val cnnLatenciesMs = Collections.synchronizedList(ArrayList<Double>())
    private var analysedFrames = 0L
    private var resultFrames = 0L
    private var benchStartNanos = 0L

    // Events (006)
    private val eventAccumulator = EventAccumulator()

    // Optional eye-local smoothing (021); no-op unless SessionConfig.filterEnabled.
    private val eyeFilter = EyeLocalFilter()

    // One-Euro smoothing of the gaze that feeds the point-of-gaze (037); always on.
    private val gazeSmoother = GazeSmoother()

    // Session (007)
    private var motionSensors: MotionSensors? = null

    // Interaction markers (012)
    @Volatile private var markerCount = 0

    // Long-run survival (013): a watchdog detects a frame stall (e.g. camera eviction by
    // another app), logs a tracking-loss interval, and attempts to re-acquire the camera.
    private var watchdog: ScheduledExecutorService? = null
    private var cameraLoss: SessionRecorder.LossInterval? = null
    @Volatile private var lastReacquireMs = 0L

    // Debug overlay (015): throttled publish of landmark points, only when enabled.
    private var lastOverlayMs = 0L

    // Quality alert (016): smoothed mean luma of the analysis frame for a low-light warning.
    @Volatile private var lumaEma = 0.0

    // Export (008)
    private var csvWriter: CsvSessionWriter? = null
    @Volatile private var lastCameraSensorTs = 0L

    // Naming (018): one stamp links session_<stamp>.csv with its meta_<stamp>.csv sidecar.
    @Volatile private var sessionStamp = 0L

    // Raw video (010)
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    @Volatile private var videoPath: String = ""

    override fun onCreate() {
        super.onCreate()
        analysisExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_MARK -> onMark()
            else -> startTracking()
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        paused = false
        markerCount = 0
        cameraLoss = null
        lastReacquireMs = 0L
        lastOverlayMs = 0L
        lumaEma = 0.0
        sessionStamp = System.currentTimeMillis()
        profile = ProbeConfig.selected
        benchStartNanos = SystemClock.elapsedRealtimeNanos()
        BenchmarkStats.reset(profile.name)
        eventAccumulator.reset()
        eyeFilter.reset()
        gazeSmoother.reset()
        GazeCnn.load(this, SessionConfig.gazeModelName) // load the selected side-loaded CNN if present (no-op otherwise)
        cnnLatenciesMs.clear()
        SummaryStats.clear()
        SessionRecorder.start(profile.name, System.currentTimeMillis(), SystemClock.elapsedRealtimeNanos())
        motionSensors = MotionSensors(this).also { it.start() }
        getExternalFilesDir(null)?.let { dir ->
            csvWriter = CsvSessionWriter(dir).apply {
                start(SessionConfig.signalSource, SessionConfig.eyeMode, System.currentTimeMillis(), sessionStamp, SessionConfig.sessionName)
            }
        }
        createChannel()
        startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        TrackingStats.onStart(SystemClock.elapsedRealtimeNanos())
        openLog()
        analysisExecutor.execute {
            faceHelper = FaceLandmarkerHelper(
                this,
                onResult = ::handleFaceResult,
                onError = { error -> Log.e(TAG, "MediaPipe: $error") },
            )
        }
        bindCamera()
        startWatchdog()
        startOrientationListener()
    }

    /** Follow device rotation so `imageInfo.rotationDegrees` stays correct as the phone turns (023). */
    private fun startOrientationListener() {
        orientationListener?.disable()
        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageAnalysis?.setTargetRotation(rotation)
            }
        }.also { if (it.canDetectOrientation()) it.enable() }
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
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(profile.resolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                        )
                        .build(),
                )
                .build()
            imageAnalysis = analysis
            analysis.setAnalyzer(analysisExecutor) { image ->
                if (paused) {
                    image.close()
                    return@setAnalyzer
                }
                val elapsed = SystemClock.elapsedRealtimeNanos()
                val index = frameIndex++
                lastCameraSensorTs = image.imageInfo.timestamp
                logFrame(index, image.imageInfo.timestamp, elapsed)
                TrackingStats.onFrame(elapsed)
                maybeUpdateNotification(index + 1)
                if (index % profile.inferenceCadence == 0L) {
                    try {
                        val ts = nextMpTimestamp()
                        submitTimesNanos[ts] = SystemClock.elapsedRealtimeNanos()
                        analysedFrames++
                        val bitmap = image.toBitmap()
                        sampleLuma(bitmap)
                        faceHelper?.detectAsync(bitmap, image.imageInfo.rotationDegrees, ts)
                    } catch (t: Throwable) {
                        Log.e(TAG, "face detect failed", t)
                    }
                }
                image.close()
            }
            try {
                provider.unbindAll()
                if (SessionConfig.rawVideoEnabled) {
                    bindWithVideo(provider, analysis)
                } else {
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "bindToLifecycle failed", t)
            }
        }, mainExecutor)
    }

    private fun bindWithVideo(provider: ProcessCameraProvider, analysis: ImageAnalysis) {
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
        val capture = VideoCapture.withOutput(recorder)
        try {
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis, capture)
            videoCapture = capture
            startVideoRecording(capture)
        } catch (t: Throwable) {
            Log.e(TAG, "video+analysis bind failed; falling back to analysis only", t)
            videoCapture = null
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }
    }

    private fun startVideoRecording(capture: VideoCapture<Recorder>) {
        if (recording != null) return // re-acquire (013) must not spawn a second recording
        try {
            val dir = getExternalFilesDir(null) ?: return
            val file = File(dir, "video_$sessionStamp.mp4")
            videoPath = file.absolutePath
            recording = capture.output
                .prepareRecording(this, FileOutputOptions.Builder(file).build())
                .start(mainExecutor) { /* VideoRecordEvent ignored for v1 */ }
            Log.i(TAG, "Recording raw video to ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "video record failed", t)
        }
    }

    /** MediaPipe LIVE_STREAM requires strictly increasing timestamps (ms). */
    private fun nextMpTimestamp(): Long {
        val now = SystemClock.uptimeMillis()
        lastMpTimestamp = if (now > lastMpTimestamp) now else lastMpTimestamp + 1
        return lastMpTimestamp
    }

    /** Override the eye-local gaze with the side-loaded CNN's per-eye output (prompt 042). */
    private fun applyCnnGaze(frame: TrackingFrameResult, result: FaceLandmarkerResult, bitmap: Bitmap): TrackingFrameResult {
        val landmarks = result.faceLandmarks().firstOrNull() ?: return frame
        val n = landmarks.size
        val xs = FloatArray(n) { landmarks[it].x() }
        val ys = FloatArray(n) { landmarks[it].y() }
        val t0 = SystemClock.elapsedRealtimeNanos()
        val l = GazePreprocessor.eyePatch(bitmap, xs, ys, GazeGeometry.LEFT_EYE)?.let { GazeCnn.infer(it) }
        val r = GazePreprocessor.eyePatch(bitmap, xs, ys, GazeGeometry.RIGHT_EYE)?.let { GazeCnn.infer(it) }
        if (l == null && r == null) return frame // no CNN gaze this frame -> keep the iris base (fallback)
        cnnLatenciesMs.add((SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000.0)
        val left = if (l != null) frame.leftEye?.copy(irisXLocal = l.second, irisYLocal = l.first) else frame.leftEye
        val right = if (r != null) frame.rightEye?.copy(irisXLocal = r.second, irisYLocal = r.first) else frame.rightEye
        return frame.copy(leftEye = left, rightEye = right)
    }

    /** Override gaze with a WebEyeTrack model's binocular point-of-gaze (prompt 049). */
    private fun applyWebEyeTrackGaze(
        frame: TrackingFrameResult,
        result: FaceLandmarkerResult,
        bitmap: Bitmap,
    ): TrackingFrameResult {
        val landmarks = result.faceLandmarks().firstOrNull() ?: return frame
        val hp = frame.headPose ?: return frame
        val n = landmarks.size
        if (n < 468) return frame // need the full face mesh for the eye-region warp + corners
        val xs = FloatArray(n) { landmarks[it].x() }
        val ys = FloatArray(n) { landmarks[it].y() }
        val t0 = SystemClock.elapsedRealtimeNanos()
        val strip = WebEyeTrackPreprocessor.eyeStrip(bitmap, xs, ys) ?: return frame
        val headVec = WebEyeTrackGeometry.headVector(
            Math.toRadians(hp.yawDeg.toDouble()).toFloat(),
            Math.toRadians(hp.pitchDeg.toDouble()).toFloat(),
        )
        // Eye centres = midpoints of the eye corners (right: 33/133, left: 362/263).
        val rxc = (xs[33] + xs[133]) * 0.5f
        val ryc = (ys[33] + ys[133]) * 0.5f
        val lxc = (xs[362] + xs[263]) * 0.5f
        val lyc = (ys[362] + ys[263]) * 0.5f
        val origin = WebEyeTrackGeometry.faceOrigin3d(lxc, lyc, rxc, ryc, bitmap.width, bitmap.height)
        val pog = GazeCnn.inferMulti(listOf(strip, headVec, origin)) ?: return frame
        cnnLatenciesMs.add((SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000.0)
        // WebEyeTrack emits one binocular PoG in [-0.5,0.5]; set both eyes so binocularGaze == PoG,
        // and the existing calibration maps it to the screen (standing in for the model's MAML head).
        val left = frame.leftEye?.copy(irisXLocal = pog[0], irisYLocal = pog[1])
        val right = frame.rightEye?.copy(irisXLocal = pog[0], irisYLocal = pog[1])
        return frame.copy(leftEye = left, rightEye = right)
    }

    /** Override gaze with an Open Gaze / Google-family model's point-of-gaze in cm (prompt 050). */
    private fun applyOpenGazeGaze(
        frame: TrackingFrameResult,
        result: FaceLandmarkerResult,
        bitmap: Bitmap,
    ): TrackingFrameResult {
        val landmarks = result.faceLandmarks().firstOrNull() ?: return frame
        val n = landmarks.size
        if (n < 468) return frame
        val xs = FloatArray(n) { landmarks[it].x() }
        val ys = FloatArray(n) { landmarks[it].y() }
        val t0 = SystemClock.elapsedRealtimeNanos()
        val leftEye = OpenGazePreprocessor.eyePatchRgb(bitmap, xs, ys, GazeGeometry.LEFT_EYE, flip = true)
            ?: return frame
        val rightEye = OpenGazePreprocessor.eyePatchRgb(bitmap, xs, ys, GazeGeometry.RIGHT_EYE, flip = false)
            ?: return frame
        val lms = OpenGazeGeometry.eyeCornerLms(xs, ys) ?: return frame
        val pog = GazeCnn.inferMulti(listOf(leftEye, rightEye, lms)) ?: return frame
        cnnLatenciesMs.add((SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000.0)
        // One binocular PoG (cm) for both eyes; calibration maps cm -> screen (its SVR/affine stand-in).
        val left = frame.leftEye?.copy(irisXLocal = pog[0], irisYLocal = pog[1])
        val right = frame.rightEye?.copy(irisXLocal = pog[0], irisYLocal = pog[1])
        return frame.copy(leftEye = left, rightEye = right)
    }

    /** Override gaze with a full-face model's output (prompt 051; UniGaze-B etc.). */
    private fun applyFullFaceGaze(
        frame: TrackingFrameResult,
        result: FaceLandmarkerResult,
        bitmap: Bitmap,
    ): TrackingFrameResult {
        val landmarks = result.faceLandmarks().firstOrNull() ?: return frame
        val n = landmarks.size
        val xs = FloatArray(n) { landmarks[it].x() }
        val ys = FloatArray(n) { landmarks[it].y() }
        val t0 = SystemClock.elapsedRealtimeNanos()
        val patch = FaceCropPreprocessor.facePatch(bitmap, xs, ys) ?: return frame
        val out = GazeCnn.infer(patch) ?: return frame // single-input [1,224,224,3] -> [1,2]
        cnnLatenciesMs.add((SystemClock.elapsedRealtimeNanos() - t0) / 1_000_000.0)
        // One binocular gaze pair for both eyes; calibration maps it (absorbing the angle convention).
        val left = frame.leftEye?.copy(irisXLocal = out.second, irisYLocal = out.first)
        val right = frame.rightEye?.copy(irisXLocal = out.second, irisYLocal = out.first)
        return frame.copy(leftEye = left, rightEye = right)
    }

    private fun handleFaceResult(result: FaceLandmarkerResult, bitmap: Bitmap) {
        val submitNanos = submitTimesNanos.remove(result.timestampMs())
        if (submitNanos != null) {
            latenciesMs.add((SystemClock.elapsedRealtimeNanos() - submitNanos) / 1_000_000.0)
            resultFrames++
            if (resultFrames % 15L == 0L) publishBenchmark()
        }
        val faces = result.faceLandmarks()
        val detected = faces.isNotEmpty()
        val landmarkCount = if (detected) faces[0].size else 0
        var blinkLeft = 0f
        var blinkRight = 0f
        val blendshapes = result.faceBlendshapes()
        if (blendshapes.isPresent && blendshapes.get().isNotEmpty()) {
            for (category in blendshapes.get()[0]) {
                when (category.categoryName()) {
                    "eyeBlinkLeft" -> blinkLeft = category.score()
                    "eyeBlinkRight" -> blinkRight = category.score()
                }
            }
        }
        TrackingStats.onFace(detected, landmarkCount, blinkLeft, blinkRight)
        maybePublishOverlay(faces, detected)
        publishQuality(detected)
        var frame = eyeFilter.process(FaceSignalAdapter.toResult(result))
        if (SessionConfig.signalSource == SessionConfig.SOURCE_CNN && GazeCnn.isAvailable) {
            frame = when (GazeCnn.activeProfile) {
                GazeModelProfile.EYE_GRAY -> applyCnnGaze(frame, result, bitmap)
                GazeModelProfile.WEB_EYE_TRACK -> applyWebEyeTrackGaze(frame, result, bitmap)
                GazeModelProfile.DUAL_EYE_POG -> applyOpenGazeGaze(frame, result, bitmap)
                GazeModelProfile.FULL_FACE -> applyFullFaceGaze(frame, result, bitmap)
                else -> frame
            }
        }
        SignalStats.update(frame)
        eventAccumulator.onResult(frame, result.timestampMs())
        val tNanos = SystemClock.elapsedRealtimeNanos()
        SessionRecorder.addSample(frame, tNanos)
        val gaze = binocularGaze(frame)?.let { (gx, gy) -> gazeSmoother.filter(gx, gy, tNanos) }
            ?: run { gazeSmoother.reset(); null }
        val pog = CalibrationStore.state.value?.let { cal ->
            gaze?.let { (gx, gy) -> cal.map(gx, gy) }
        }
        GazeStats.update(pog)
        csvWriter?.appendSample(frame, tNanos, lastCameraSensorTs, "unknown", pog?.first, pog?.second)
        SessionStats.update(
            SessionSummary(
                recording = true,
                sampleCount = SessionRecorder.samples.size,
                sensorSampleCount = SessionRecorder.sensorSamples.size,
                lossIntervalCount = SessionRecorder.lossIntervals.size,
                sensorsActive = motionSensors?.active == true,
                markerCount = markerCount,
            ),
        )
    }

    /** Publish landmark points for the debug overlay, throttled, only when enabled (015). */
    private fun maybePublishOverlay(faces: List<List<NormalizedLandmark>>, detected: Boolean) {
        if (!OverlayConfig.enabled) return
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastOverlayMs < OVERLAY_INTERVAL_MS) return
        lastOverlayMs = nowMs
        if (!detected) {
            OverlayStats.clear()
            return
        }
        val lm = faces[0]
        val arr = FloatArray(lm.size * 2)
        for (i in lm.indices) {
            arr[i * 2] = lm[i].x()
            arr[i * 2 + 1] = lm[i].y()
        }
        OverlayStats.update(
            OverlayFrame(arr, FaceMeshIndices.IMG_LEFT_IRIS_CENTRE, FaceMeshIndices.IMG_RIGHT_IRIS_CENTRE),
        )
    }

    /** Derive the advisory quality label from face detection + smoothed luma (016). */
    private fun publishQuality(detected: Boolean) {
        val label = when {
            !detected -> QualitySnapshot.FACE_LOST
            lumaEma > 0.0 && lumaEma < LOW_LIGHT_LUMA -> QualitySnapshot.LOW_LIGHT
            else -> QualitySnapshot.GOOD
        }
        QualityStats.update(label, lumaEma)
    }

    private fun sampleLuma(bitmap: Bitmap) {
        val luma = meanLuma(bitmap)
        lumaEma = if (lumaEma <= 0.0) luma else LUMA_EMA_ALPHA * luma + (1.0 - LUMA_EMA_ALPHA) * lumaEma
    }

    /** Mean luma (0–255) over a coarse 16×16 grid of the analysis frame. */
    private fun meanLuma(bitmap: Bitmap): Double {
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var sum = 0.0
        var n = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val p = bitmap.getPixel(x, y)
                sum += 0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)
                n++
                x += stepX
            }
            y += stepY
        }
        return if (n > 0) sum / n else 0.0
    }

    private fun publishBenchmark() {
        val elapsedSec = (SystemClock.elapsedRealtimeNanos() - benchStartNanos) / 1_000_000_000.0
        val snapshot = synchronized(latenciesMs) {
            val sorted = latenciesMs.sorted()
            val n = sorted.size
            if (n == 0) return
            BenchmarkSnapshot(
                profileName = profile.name,
                analysedFrames = analysedFrames,
                droppedFrames = (analysedFrames - resultFrames).coerceAtLeast(0L),
                analysedFps = if (elapsedSec > 0.5) resultFrames / elapsedSec else 0.0,
                latencyMeanMs = sorted.sum() / n,
                latencyP50Ms = sorted[(n * 50 / 100).coerceIn(0, n - 1)],
                latencyP95Ms = sorted[(n * 95 / 100).coerceIn(0, n - 1)],
            )
        }
        BenchmarkStats.update(snapshot)
    }

    private fun writeBenchmarkCsv() {
        if (analysedFrames == 0L) return
        try {
            publishBenchmark()
            val snap = BenchmarkStats.state.value
            val file = File(getExternalFilesDir(null), "benchmark_$sessionStamp.csv")
            file.bufferedWriter().use { w ->
                w.write("profile,analysed_frames,dropped_frames,analysed_fps,latency_mean_ms,latency_p50_ms,latency_p95_ms")
                w.newLine()
                w.write(
                    listOf(
                        snap.profileName,
                        snap.analysedFrames.toString(),
                        snap.droppedFrames.toString(),
                        fmt(snap.analysedFps),
                        fmt(snap.latencyMeanMs),
                        fmt(snap.latencyP50Ms),
                        fmt(snap.latencyP95Ms),
                    ).joinToString(","),
                )
                w.newLine()
                w.newLine()
                w.write("latency_ms")
                w.newLine()
                synchronized(latenciesMs) {
                    for (latency in latenciesMs) {
                        w.write(fmt(latency))
                        w.newLine()
                    }
                }
                synchronized(cnnLatenciesMs) {
                    if (cnnLatenciesMs.isNotEmpty()) {
                        val sorted = cnnLatenciesMs.sorted()
                        val nC = sorted.size
                        w.newLine()
                        w.write("cnn_inference_mean_ms,cnn_inference_p50_ms,cnn_inference_p95_ms,cnn_frames")
                        w.newLine()
                        w.write(
                            listOf(
                                fmt(sorted.sum() / nC),
                                fmt(sorted[(nC * 50 / 100).coerceIn(0, nC - 1)]),
                                fmt(sorted[(nC * 95 / 100).coerceIn(0, nC - 1)]),
                                nC.toString(),
                            ).joinToString(","),
                        )
                        w.newLine()
                    }
                }
            }
            Log.i(TAG, "Benchmark written: ${file.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "benchmark write failed", t)
        }
    }

    private fun fmt(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

    private fun finalizeSessionCsv() {
        val writer = csvWriter ?: return
        val samples = SessionRecorder.samples.toList()
        val saccadeSamples = samples.map { s ->
            val x = if (!s.leftX.isNaN()) s.leftX else s.rightX
            val y = if (!s.leftY.isNaN()) s.leftY else s.rightY
            SaccadeSample(
                s.tElapsedNanos / 1_000_000, x, y, s.leftReliability,
                s.leftBlink == BlinkState.CLOSED || s.leftBlink == BlinkState.CLOSING, true,
            )
        }
        val saccades = SaccadeDetector.detect(saccadeSamples)
        val fixations = FixationDetector.detect(saccadeSamples)
        val blinkSamples = samples.map { BlinkSample(it.tElapsedNanos / 1_000_000, it.leftBlink) }
        val blinks = BlinkDetector.detect(blinkSamples)
        for (event in saccades) writer.appendSaccade(event)
        for (event in fixations) writer.appendFixation(event)
        for (event in blinks) writer.appendBlink(event)
        val file = writer.finalizeSession()
        csvWriter = null
        Log.i(TAG, "Session CSV: ${file?.absolutePath}")
        writeSensorSidecar()
        writeMetaSidecar()
        writeSummarySidecar(samples, saccades, fixations, blinks)
    }

    /** Compute per-session summary stats (prompt 020): publish to the UI and a summary sidecar. */
    private fun writeSummarySidecar(
        samples: List<SessionRecorder.Sample>,
        saccades: List<SaccadeEvent>,
        fixations: List<FixationEvent>,
        blinks: List<BlinkEvent>,
    ) {
        try {
            val durationMs = if (samples.size >= 2) {
                (samples.last().tElapsedNanos - samples.first().tElapsedNanos) / 1_000_000
            } else {
                0L
            }
            val durationMin = durationMs / 60_000.0
            fun rate(count: Int) = if (durationMin > 0) count / durationMin else 0.0
            val lossMs = SessionRecorder.lossIntervals.sumOf { (it.endNanos - it.startNanos) / 1_000_000 }
            val reliabilities = samples.map { it.faceReliability.toDouble() }.filter { !it.isNaN() }
            val sacAmp = saccades.map { it.amplitude }
            val sacDur = saccades.map { it.durationMs.toDouble() }
            val fixDur = fixations.map { it.durationMs.toDouble() }
            val stats = SessionSummaryStats(
                durationSec = durationMs / 1000.0,
                saccades = saccades.size, saccadeRatePerMin = rate(saccades.size),
                meanSaccadeAmplitude = mean(sacAmp), medianSaccadeAmplitude = median(sacAmp),
                meanSaccadeDurationMs = mean(sacDur), medianSaccadeDurationMs = median(sacDur),
                fixations = fixations.size, fixationRatePerMin = rate(fixations.size),
                meanFixationDurationMs = mean(fixDur),
                blinks = blinks.size, blinkRatePerMin = rate(blinks.size),
                meanReliability = mean(reliabilities), trackingLossSec = lossMs / 1000.0,
            )
            SummaryStats.update(stats)
            File(getExternalFilesDir(null), "summary_$sessionStamp.csv").bufferedWriter().use { w ->
                w.write("key,value"); w.newLine()
                w.write("duration_sec,${fmt(stats.durationSec)}"); w.newLine()
                w.write("saccades,${stats.saccades}"); w.newLine()
                w.write("saccade_rate_per_min,${fmt(stats.saccadeRatePerMin)}"); w.newLine()
                w.write("mean_saccade_amplitude,${fmt(stats.meanSaccadeAmplitude)}"); w.newLine()
                w.write("median_saccade_amplitude,${fmt(stats.medianSaccadeAmplitude)}"); w.newLine()
                w.write("mean_saccade_duration_ms,${fmt(stats.meanSaccadeDurationMs)}"); w.newLine()
                w.write("median_saccade_duration_ms,${fmt(stats.medianSaccadeDurationMs)}"); w.newLine()
                w.write("fixations,${stats.fixations}"); w.newLine()
                w.write("fixation_rate_per_min,${fmt(stats.fixationRatePerMin)}"); w.newLine()
                w.write("mean_fixation_duration_ms,${fmt(stats.meanFixationDurationMs)}"); w.newLine()
                w.write("blinks,${stats.blinks}"); w.newLine()
                w.write("blink_rate_per_min,${fmt(stats.blinkRatePerMin)}"); w.newLine()
                w.write("mean_reliability,${fmt(stats.meanReliability)}"); w.newLine()
                w.write("tracking_loss_sec,${fmt(stats.trackingLossSec)}"); w.newLine()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "summary sidecar failed", t)
        }
    }

    private fun mean(xs: List<Double>): Double = if (xs.isEmpty()) 0.0 else xs.average()

    private fun median(xs: List<Double>): Double {
        if (xs.isEmpty()) return 0.0
        val s = xs.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
    }

    private fun writeMetaSidecar() {
        try {
            val file = File(getExternalFilesDir(null), "meta_$sessionStamp.csv")
            file.bufferedWriter().use { w ->
                w.write("key,value"); w.newLine()
                w.write("profile,${SessionRecorder.profileName}"); w.newLine()
                w.write("session_name,${SessionConfig.sessionName}"); w.newLine()
                w.write("session_note,${SessionConfig.sessionNote.replace('\n', ' ').replace('\r', ' ')}"); w.newLine()
                w.write("use_case_mode,${SessionConfig.useCaseMode}"); w.newLine()
                w.write("eye_mode,${SessionConfig.eyeMode}"); w.newLine()
                w.write("signal_source,${SessionConfig.signalSource}"); w.newLine()
                w.write("gaze_model,${GazeCnn.activeModel}"); w.newLine()
                w.write("calibrated,${CalibrationStore.state.value != null}"); w.newLine()
                w.write("calibration_error,${CalibrationStore.error.value ?: ""}"); w.newLine()
                w.write("raw_video_enabled,${SessionConfig.rawVideoEnabled}"); w.newLine()
                w.write("filter_enabled,${SessionConfig.filterEnabled}"); w.newLine()
                w.write("filter_alpha,${SessionConfig.filterAlpha}"); w.newLine()
                w.write("raw_video_path,$videoPath"); w.newLine()
                w.write("start_wallclock_ms,${SessionRecorder.startWallClockMs}"); w.newLine()
                w.write("start_elapsed_nanos,${SessionRecorder.startElapsedNanos}"); w.newLine()
                w.write("stop_wallclock_ms,${System.currentTimeMillis()}"); w.newLine()
                w.write("sample_count,${SessionRecorder.samples.size}"); w.newLine()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "meta sidecar failed", t)
        }
    }

    private fun writeSensorSidecar() {
        val sensors = SessionRecorder.sensorSamples.toList()
        if (sensors.isEmpty()) return
        try {
            val file = File(getExternalFilesDir(null), "sensors_$sessionStamp.csv")
            file.bufferedWriter().use { w ->
                w.write("sensor,timestamp_nanos,v0,v1,v2")
                w.newLine()
                for (s in sensors) {
                    w.write("${s.sensor},${s.tNanos},${fmt(s.v0.toDouble())},${fmt(s.v1.toDouble())},${fmt(s.v2.toDouble())}")
                    w.newLine()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "sensor sidecar failed", t)
        }
    }

    private fun openLog() {
        try {
            val file = File(getExternalFilesDir(null), "frame_log_$sessionStamp.csv")
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
            val videoSuffix = if (recording != null) " · ● REC video" else ""
            val qualitySuffix = when (QualityStats.state.value.label) {
                QualitySnapshot.LOW_LIGHT -> " · ⚠ Low light"
                QualitySnapshot.FACE_LOST -> " · ⚠ No face"
                else -> ""
            }
            val warning = TrackingStats.state.value.resourceWarning
            val resourceSuffix = if (warning.isNotEmpty()) " · ⚠ $warning" else ""
            manager.notify(
                NOTIF_ID,
                buildNotification("Frames logged: $frameCount$videoSuffix$qualitySuffix$resourceSuffix. Tap Stop to end."),
            )
        } catch (t: Throwable) {
            Log.e(TAG, "notification update failed", t)
        }
    }

    /** Pause: keep the service + camera bound, but drop frames (no logging/inference/recording). */
    private fun pause() {
        if (paused) return
        paused = true
        try {
            recording?.pause()
        } catch (t: Throwable) {
            Log.e(TAG, "video pause failed", t)
        }
        TrackingStats.onPause()
        refreshNotification()
    }

    /** Resume a paused session (same session, same files). */
    private fun resume() {
        if (!paused) return
        paused = false
        try {
            recording?.resume()
        } catch (t: Throwable) {
            Log.e(TAG, "video resume failed", t)
        }
        TrackingStats.onResume()
        refreshNotification()
    }

    private fun refreshNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val text = when {
                paused -> "Paused. Tap Resume to continue."
                TrackingStats.state.value.cameraLost -> "Camera lost — retrying…"
                else -> "Recording. Tap Stop to end."
            }
            manager.notify(NOTIF_ID, buildNotification(text))
        } catch (t: Throwable) {
            Log.e(TAG, "notification refresh failed", t)
        }
    }

    /** Periodic check for a frame stall (camera eviction); logs the loss and re-acquires (013). */
    private fun startWatchdog() {
        watchdog?.shutdownNow()
        watchdog = Executors.newSingleThreadScheduledExecutor().also { exec ->
            exec.scheduleWithFixedDelay(
                {
                    try {
                        checkFrameHealth()
                        checkResources()
                    } catch (t: Throwable) {
                        Log.e(TAG, "watchdog tick failed", t)
                    }
                },
                FRAME_WATCHDOG_INTERVAL_MS,
                FRAME_WATCHDOG_INTERVAL_MS,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    private fun checkFrameHealth() {
        if (paused) return
        val last = TrackingStats.state.value.lastFrameElapsedRealtimeNanos
        if (last <= 0L) return
        val gapMs = (SystemClock.elapsedRealtimeNanos() - last) / 1_000_000
        if (gapMs > FRAME_LOSS_THRESHOLD_MS) onCameraLoss(gapMs) else onCameraRecovered()
    }

    private fun onCameraLoss(gapMs: Long) {
        val now = SystemClock.elapsedRealtimeNanos()
        val existing = cameraLoss
        if (existing == null) {
            cameraLoss = SessionRecorder.LossInterval(now, now).also { SessionRecorder.lossIntervals.add(it) }
            TrackingStats.onCameraLost(true)
            refreshNotification()
            Log.w(TAG, "camera frame stall (${gapMs} ms) — attempting re-acquire")
        } else {
            existing.endNanos = now
        }
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastReacquireMs >= REACQUIRE_INTERVAL_MS) {
            lastReacquireMs = nowMs
            bindCamera()
        }
    }

    private fun onCameraRecovered() {
        if (cameraLoss != null) {
            cameraLoss?.endNanos = SystemClock.elapsedRealtimeNanos()
            cameraLoss = null
            TrackingStats.onCameraLost(false)
            refreshNotification()
            Log.i(TAG, "camera frames recovered")
        }
    }

    /** Advisory storage + thermal guards (024): warn, and on a critical condition auto-stop safely. */
    private fun checkResources() {
        if (csvWriter == null) return
        val path = (getExternalFilesDir(null) ?: filesDir).path
        val freeBytes = try {
            StatFs(path).availableBytes
        } catch (t: Throwable) {
            Long.MAX_VALUE
        }
        val thermal = try {
            (getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
        } catch (t: Throwable) {
            PowerManager.THERMAL_STATUS_NONE
        }
        if (freeBytes < STORAGE_CRITICAL_BYTES || thermal >= PowerManager.THERMAL_STATUS_CRITICAL) {
            val reason = if (freeBytes < STORAGE_CRITICAL_BYTES) "storage critically low" else "device overheating"
            TrackingStats.onResourceWarning("Auto-stopped: $reason")
            Log.w(TAG, "resource guard auto-stop: $reason")
            mainExecutor.execute { if (csvWriter != null) stopTracking() }
            return
        }
        val warning = when {
            freeBytes < STORAGE_WARN_BYTES -> "Low storage — consider stopping soon"
            thermal >= PowerManager.THERMAL_STATUS_SEVERE -> "Device hot — may throttle"
            else -> ""
        }
        TrackingStats.onResourceWarning(warning)
    }

    /** Record a user interaction marker as a `task` row at the current canonical timestamp (012). */
    private fun onMark() {
        val writer = csvWriter ?: return // ignored when no session is active
        val now = SystemClock.elapsedRealtimeNanos()
        markerCount++
        writer.appendTask("mark", "", now)
        SessionStats.update(
            SessionSummary(
                recording = true,
                sampleCount = SessionRecorder.samples.size,
                sensorSampleCount = SessionRecorder.sensorSamples.size,
                lossIntervalCount = SessionRecorder.lossIntervals.size,
                sensorsActive = motionSensors?.active == true,
                markerCount = markerCount,
            ),
        )
        Log.i(TAG, "marker #$markerCount at $now")
    }

    private fun stopTracking() {
        GazeCnn.close()
        writeBenchmarkCsv()
        finalizeSessionCsv()
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
        watchdog?.shutdownNow()
        watchdog = null
        cameraLoss = null
        orientationListener?.disable()
        orientationListener = null
        imageAnalysis = null
        try {
            recording?.stop()
        } catch (t: Throwable) {
            Log.e(TAG, "video stop failed", t)
        }
        recording = null
        videoCapture = null
        videoPath = ""
        try {
            cameraProvider?.unbindAll()
        } catch (t: Throwable) {
            Log.e(TAG, "unbind failed", t)
        }
        cameraProvider = null
        faceHelper?.close()
        faceHelper = null
        try {
            logWriter?.flush()
            logWriter?.close()
        } catch (t: Throwable) {
            Log.e(TAG, "close log failed", t)
        }
        logWriter = null
        TrackingStats.onStop()
        SignalStats.clear()
        OverlayStats.clear()
        GazeStats.clear()
        QualityStats.clear()
        eventAccumulator.reset()
        motionSensors?.stop()
        motionSensors = null
        SessionRecorder.stop(System.currentTimeMillis())
        SessionStats.update(
            SessionSummary(
                recording = false,
                sampleCount = SessionRecorder.samples.size,
                sensorSampleCount = SessionRecorder.sensorSamples.size,
                lossIntervalCount = SessionRecorder.lossIntervals.size,
                sensorsActive = false,
                markerCount = markerCount,
            ),
        )
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
            .setContentTitle(if (paused) "Saccadacus tracking paused" else "Saccadacus tracking active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (paused) {
                    NotificationCompat.Action(0, "Resume", servicePendingIntent(ACTION_RESUME, 2))
                } else {
                    NotificationCompat.Action(0, "Pause", servicePendingIntent(ACTION_PAUSE, 1))
                },
            )
            .addAction(0, "Mark", servicePendingIntent(ACTION_MARK, 3))
            .addAction(0, "Stop", servicePendingIntent(ACTION_STOP, 0))
            .build()

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, CameraTrackingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_START = "com.example.saccadacusandroid.action.START"
        const val ACTION_STOP = "com.example.saccadacusandroid.action.STOP"
        const val ACTION_PAUSE = "com.example.saccadacusandroid.action.PAUSE"
        const val ACTION_RESUME = "com.example.saccadacusandroid.action.RESUME"
        const val ACTION_MARK = "com.example.saccadacusandroid.action.MARK"
        private const val CHANNEL_ID = "saccadacus_tracking"
        private const val NOTIF_ID = 1
        private const val NOTIF_UPDATE_INTERVAL_MS = 2000L
        private const val FRAME_WATCHDOG_INTERVAL_MS = 2000L
        private const val FRAME_LOSS_THRESHOLD_MS = 4000L
        private const val REACQUIRE_INTERVAL_MS = 3000L
        private const val OVERLAY_INTERVAL_MS = 100L
        private const val LOW_LIGHT_LUMA = 60.0
        private const val LUMA_EMA_ALPHA = 0.2
        private const val STORAGE_WARN_BYTES = 200L * 1024 * 1024
        private const val STORAGE_CRITICAL_BYTES = 50L * 1024 * 1024
        private const val TAG = "SaccadacusFGS"
    }
}
