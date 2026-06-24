# Current state

Living, high-level orientation: what exists now, key decisions, and what is in
progress. Read it at session start. Records current reality; the binding canon is
`docs-dev/reference/primary_authoritative/specification.md` (the canon wins on conflict).

## Systems

- **Camera foreground-service probe (prompts 002 / 002b)** — a `camera`-type
  `LifecycleService` runs CameraX front-camera `ImageAnalysis` and logs frame timing
  (`frame_index, camera_sensor_timestamp, elapsed_realtime_nanos`) to a CSV in the app's
  external files dir. A persistent notification (with Stop) and the activity show a live
  frame count + "since last frame" + verdict, so the survival test needs no `adb`/CSV.
  No tracking/ML yet. Built green in CI; **on-device frame survival CONFIRMED on one
  device (2026-06-24): frames kept arriving while the app was backgrounded / screen
  locked.**
- Compose UI: a minimal Start/Stop screen (no theming work beyond the scaffold).

## Key decisions

The stack and architecture are now settled (see `specification.md` §Locked decisions),
from the Android-stack research:

- Stack: **CameraX + MediaPipe Tasks Vision Face Landmarker + Kotlin pipeline + incremental
  CSV**, all on-device with **no Google Play Services** dependency.
- A **`camera` foreground service owns the camera + model**; tracking is user-initiated
  while visible; the browser repo's **algorithms and data model** are ported (it has no
  production tracker to lift).
- **True pupil tracking deferred** (iris-centre only); raw video off by default.
- **Decided:** `minSdk 30` (Android 11; the camera FGS *type* requires API 30),
  `applicationId com.saccadacus.android` (see spec).

Supporting research (non-binding) lives in `docs-dev/planning/`:
`android_stack_research_brief.md`, `saccadacus_phase0_audit.md`,
`android_stack_research_report.md` (the latter's §19 is the staged implementation plan).

## In progress / next

- **Feasibility confirmed (1 device, 2026-06-24)** — background front-camera frames
  persist; the core approach is viable. Optional later: a 30–60 min run on a
  Samsung/Xiaomi to check aggressive battery-killers. **Prompt 003 is unblocked.**
- Debug APKs are published to the GitHub **Releases → latest** page on every push to
  `main` (one-tap sideload), in addition to the per-run Actions artifact.
- **Prompt queue `003`–`010` is drafted** in `docs-dev/agent/prompts/`: engine (003) →
  on-device benchmark (004) → eye/iris/head-pose adapter (005) → signals/events + parity
  tests (006) → session + sensors (007) → CSV export (008) → modes + quality UI (009) →
  optional raw video (010). **All of `003`–`010` are implemented and CI-green; the whole
  v1 pipeline now exists. On-device behaviour (does tracking actually work; are the
  eye-movement signs/mirroring correct) is the outstanding step — a device test, with the
  `SignConvention` flags ready for one-line calibration if an axis is flipped.**
- Workflow: committing and pushing **directly to `main`** (per `AGENTS.md` conventions);
  the earlier feature-branch staging is retired.

## Prompts run

- `001_setup.md` — initial scaffold confirmed.
- `002_camera_fgs_poc.md` — camera-FGS frame-logger probe (CI green, `00539b8`).
- `002b_onscreen_frame_counter.md` — on-screen/notification frame counter for an
  adb-free survival test.
- `003_mediapipe_face_landmarker.md` — MediaPipe Face Landmarker integrated (CI green,
  on-device behaviour pending).
- `004_backend_benchmark.md` — on-device inference benchmark + quality/balanced/battery
  profiles (CI green).
- `005_tracking_adapter.md` — landmarks → eye-local/iris/head-pose/blink; signs isolated
  in `SignConvention` (CI green; on-device sign check pending).
- `006_signals_events_parity.md` — saccade/blink detectors + ring buffer + JUnit parity
  tests running in CI (CI green, tests passing).
- `007_session_and_sensors.md` — in-memory session + motion sensors + tracking-loss (CI green).
- `008_csv_export.md` — incremental combined CSV + sensor/meta sidecars + share (CI green).
- `009_modes_and_quality_ui.md` — use-case/eye selectors + quality panel (CI green).
- `010_optional_raw_video.md` — consent-gated CameraX VideoCapture (CI green).
