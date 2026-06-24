# Task: Camera foreground-service proof-of-feasibility

## Goal

Prove that a `camera`-type foreground service can keep delivering front-camera frames
after the user leaves the app, by building a minimal frame-logger — no ML, no real UI.

## Scope

Implement only the work described in this prompt. This is the risk-first stage (research
report §19, Stage 1): retire the OEM/screen-off survival risk before any tracking code is
built on top. Do not implement adjacent systems or future prompts.

## Context

The binding canon is `docs-dev/reference/primary_authoritative/specification.md`
(stack, SDK levels, initiation rule). The manifest currently declares no permissions or
services — all of this is new. The acceptance test is an on-device measurement a human
must run; CI only proves the APK builds.

## Required changes

1. **Manifest & build:** add `CAMERA`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA`,
   and `POST_NOTIFICATIONS` permissions; declare a `<service>` with
   `android:foregroundServiceType="camera"`. Set `minSdk = 30` and `applicationId =
   "com.saccadacus.android"` (per spec; camera FGS type requires API 30), and
   `android:allowBackup="false"`.
2. **CameraX deps:** add CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`) to
   `gradle/libs.versions.toml` and `app/build.gradle.kts`.
3. **Foreground service:** create a notification channel; `startForeground(...,
   FOREGROUND_SERVICE_TYPE_CAMERA)` with a persistent notification carrying a **Stop**
   action; bind CameraX `ImageAnalysis` on the **front** camera with
   `STRATEGY_KEEP_ONLY_LATEST`; for each analysed frame, append
   `frameIndex, cameraSensorTimestamp, elapsedRealtimeNanos` to a CSV file in
   `getExternalFilesDir(...)`, and log the file path. Close the image promptly each frame.
4. **Activity:** a single **Start** control that requests `CAMERA` + `POST_NOTIFICATIONS`
   at runtime and, once granted, starts the service **while the activity is visible**;
   plus a **Stop** control. Keep the UI minimal — no theming or Compose polish.

## Do not implement

Do not implement:
- MediaPipe / any tracking, landmarks, or signal/event processing;
- the real session model, the export CSV schema, pause/resume, or raw-video capture;
- settings, navigation, theming, or quality-feedback UI.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` produces `app-debug.apk` (in CI);
- no camera or service code runs until the user presses **Start** (started only while the
  activity is visible);
- the persistent notification + system camera indicator appear while running, and **Stop**
  ends the service and camera use;
- **(human, on-device)** starting while visible then switching to another app and locking
  the screen leaves the frame-log file **still gaining rows for ≥30 min** on a Pixel and a
  Samsung device — record the result in `current_state.md`.

## Checks

Run `./gradlew assembleDebug --no-daemon`. Then perform the on-device survival test above
on at least one Pixel and one Samsung device and note the outcome.

## Commit and push

If and only if the scope was followed and the build passes, create one commit on `main`
using this file's exact filename (`002_camera_fgs_poc.md`) as the commit message, then push.

Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
