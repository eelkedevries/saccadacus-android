# Specification

**Version:** 0.3 · **Last updated:** 2026-06-25

The authoritative design canon for this project. Only filled sections are binding;
an empty section means "not yet decided". When a decision changes, update the
relevant section and bump the version.

*Changed in 0.3:* scope in gaze **calibration**, an eye-look-**blendshape** gaze source,
and a calibrated **point-of-gaze** (new `gaze_screen_x/y` columns; `tracking_mode ∈
{iris, blendshape}`).
*Changed in 0.2:* first real fill, from the Android-stack research
(`docs-dev/planning/android_stack_research_report.md`, non-binding analysis).

## Scope

v1 is a **native Android app that records eye- and head-movement data from the front
camera during real smartphone use**, via a user-visible camera foreground service,
processing locally and exporting CSV.

**In scope (v1):** front-camera tracking; background operation through a `camera`
foreground service; local on-device processing; persistent-notification controls
(pause/resume/stop); live tracking-quality feedback; session recording; CSV export;
optional, consent-gated raw-video storage.

**In scope (v1.x):** gaze **calibration** (an affine map fitted from on-screen targets)
producing a calibrated, normalised **point-of-gaze**; an eye-look-**blendshape** gaze
source alongside iris-centre. Accuracy is contingent on the underlying gaze signal.

**Out of scope (v1):** true pupil-centre tracking (iris-centre only — see Domain
rules); release signing and Play-Store distribution; automated cross-app
synchronisation beyond timestamp anchors + interaction markers.

## Architecture

Kotlin + Jetpack Compose. A **`camera`-type foreground service owns the camera and the
ML model**; the Compose Activity is an observer that may not own the camera.

Pipeline: **CameraX `ImageAnalysis`** (front, previewless, latest-frame) → **MediaPipe
Tasks Vision Face Landmarker** (self-contained, no Google Play Services) → a **MediaPipe
adapter** (landmarks → eye-local / iris / head-pose; front-camera mirroring resolved
here) → **Kotlin signal/event processing** ported from the `saccadacus` browser
algorithms → **pre-allocated ring buffers** → **incrementally written CSV**. The UI
observes a downsampled quality summary via `StateFlow`; the notification carries
pause/resume/stop. Non-binding detail: `docs-dev/planning/android_stack_research_report.md`.

## Data schemas

**Canonical clock:** `SystemClock.elapsedRealtimeNanos()` (monotonic). Each session also
stores a wall-clock anchor (`System.currentTimeMillis()` ↔ `elapsedRealtimeNanos()`).

**Export:** a single combined CSV; one row per `row_type ∈ {time_series, event, task}`;
locale-independent formatting (`Locale.ROOT`, fixed decimals). Column set (adapted from
the browser schema, with Android timestamp columns):

```
elapsed_realtime_nanos, camera_sensor_timestamp, camera_timestamp_source, wallclock_anchor_ms,
row_type, tracking_mode, eye_selection_mode,
left_eye_x_local, left_eye_y_local, right_eye_x_local, right_eye_y_local,
binocular_x_local, binocular_y_local,
left_eye_reliability, right_eye_reliability, iris_reliability, pupil_reliability,
head_yaw, head_pitch, head_roll, head_translation_x, head_translation_y, head_translation_z,
blink_state,
event_type, event_onset, event_offset, event_duration, event_direction,
event_relative_amplitude, event_confidence, event_head_motion_label,
task_marker, annotation,
gaze_screen_x, gaze_screen_y
```

`tracking_mode ∈ {iris, blendshape}` records the gaze source. `gaze_screen_x/y` hold a
calibrated, normalised (0–1) point-of-gaze and are empty when uncalibrated (appended at the
end of the column set so existing column positions are unchanged). `pupil_*` columns exist
but are **not populated in v1** (iris-centre only). Raw video is never written unless the
user explicitly enables it.

## Domain rules

- **Eye-local frame:** axis `u` = participant-left corner → participant-right corner
  (positive `x_local` = participant's right); `v` = `u` rotated 90° (positive `y_local` =
  up); origin = corner midpoint; normalised by inter-corner distance ("eye-width units").
  **Front-camera mirroring/handedness is resolved in the backend adapter** before
  projection; the projection itself is sign-stable.
- **Signal sources:** the eye-local gaze signal may come from the **iris-centre** (default)
  or from MediaPipe **eye-look blendshapes** (`eyeLookIn/Out/Up/Down`, per eye), which are
  more robust in low light; `tracking_mode` records which. Both populate the same eye-local
  columns in a participant-consistent frame.
- **Calibration:** an optional **affine map**, fitted from gaze captured at known on-screen
  targets, produces a calibrated, normalised (0–1) **point-of-gaze** (`gaze_screen_x/y`); the
  map subsumes the front-camera sign/scale conventions. Calibration is user-initiated and
  persisted; its accuracy depends on the underlying gaze signal.
- **Saccade detection** (eye-width units; thresholds configurable): onset speed 1.0,
  offset speed 0.4 (per second); duration 8–200 ms; minimum amplitude 0.03; binocular
  consistency weight 1.0 (consistent) / 0.6 (inconsistent); confidence =
  clamp(mean reliability × consistency × head-motion factor). Head-motion labels:
  `saccade_head_still` / `saccade_during_head_movement` / `uncertain_head_motion`.
- **Blink detection:** state machine over `BlinkState {open, closing, closed, opening,
  unknown}`; a contiguous non-open run bracketed by open is a blink; confidence 0.9 if it
  reached `closed`, else 0.6.
- **Pupil vs iris:** v1 records **iris-centre** only; a true pupil centre is not available
  from a front RGB camera and is deferred. Never relabel an iris landmark as a pupil.
- **Initiation:** tracking is always **user-initiated while the app is visible**; never
  silent or automatic; the system camera indicator and the foreground-service
  notification are always shown.

## Naming and voice

British English in code, comments, and user-facing text. Reverse-domain `applicationId`.
User-facing text: clear, neutral, research-appropriate.

## Locked decisions

- **Stack:** CameraX + MediaPipe Tasks Vision Face Landmarker + Kotlin signal pipeline +
  incremental CSV. Fallbacks: Camera2 (if CameraX can't drive a previewless service-owned
  front camera / expose the timestamp source); ML Kit Face Detection (bundled, no Play
  Services) as a degraded tracker (loses iris).
- **SDK:** `minSdk 30` (Android 11) — **decided**: the `camera` foreground-service
  *type* was added in API 30 (the `foregroundServiceType` attribute exists from API 29,
  but the `camera` value requires 30), so 30 is the true floor; it still covers
  essentially all in-use devices. Chosen over 34 (needlessly excludes Android 11–13
  phones) and over a lower floor (camera FGS type unavailable). `targetSdk 35` → 36 once
  confirmed stable; `compileSdk` per scaffold (**verify `37`/AGP `9.2.0` are stable, not
  preview** when the build is next touched).
- **True pupil-centre tracking deferred from v1** (iris-centre only).
- **No raw video by default;** optional, consent-gated. **Off-Play research distribution.**
- **`applicationId`:** `com.saccadacus.android` (decided; change only if a specific
  domain/brand is preferred).
- **Verify command:** `./gradlew assembleDebug --no-daemon` (CI builds the debug APK).
- **Largest open risk** (not a decision): OEM/screen-off frame survival — settled only by
  on-device testing (the prompt 002 proof-of-feasibility).
