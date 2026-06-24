# Task: Integrate MediaPipe Face Landmarker

## Goal

Feed the existing front-camera frames to the MediaPipe Tasks Vision Face Landmarker so
each frame yields face landmarks, blendshapes, and the head-pose matrix on-device — with
no derived signals yet.

## Scope

Wire the ML model into the camera service's frame feed only. Do not derive eye/iris/head
signals or events (that is prompt 005). One reviewable unit.

## Context

Builds on the camera foreground service from `002`/`002b`. The binding stack is in
`specification.md` §Architecture (MediaPipe Tasks Vision Face Landmarker, on-device, **no
Google Play Services**). Keep the frame-timing CSV from 002 working.

## Required changes

1. Add `com.google.mediapipe:tasks-vision` to `gradle/libs.versions.toml` and the app
   dependencies (pin the version).
2. Provide the Face Landmarker `.task` model in `app/src/main/assets/` — preferably via a
   pinned Gradle download task (URL + checksum); commit the asset only if a build-time
   download is unreliable. Record the model-asset **licence** per `specification.md` §17.
3. In the service, create a `FaceLandmarker` in **LIVE_STREAM** mode with face blendshapes
   and the facial transformation matrix enabled; convert each `ImageAnalysis` frame to the
   MediaPipe input and call `detectAsync` with the frame timestamp; handle results in the
   async result listener (do not block the analyzer thread).
4. Surface minimal live state (via `TrackingStats`/UI): face detected (yes/no), landmark
   count, and the left/right eye-blink blendshape scores.

## Do not implement

Do not implement: eye-local/iris/head-pose derivation, saccade/blink/event logic, the
session model, export-schema changes, tracking modes, or raw-video capture.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: the screen shows "face detected", roughly 478 landmarks, and the blink
  blendshape scores rise when you blink; the 002 frame-timing log still grows.
- Inference runs only while a session is active (started while visible, as before).

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`003_mediapipe_face_landmarker.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
