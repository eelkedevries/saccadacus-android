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
  No tracking/ML yet. Built green in CI; **on-device frame-survival result still pending.**
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

- **Awaiting the human on-device check** for 002/002b: install the APK, Start, switch
  away / lock the screen for ~1–2 min, return, and read whether the on-screen frame count
  kept rising (and "since last frame" is small). One phone is enough for a first pass;
  the longer multi-OEM run (Samsung/Xiaomi, 30–60 min) is the follow-up. Record the
  result here. This gates prompt 003.
- Remaining stages 003–010 are proposed in the research report §19; each will be drafted
  as a prompt when run.

## Prompts run

- `001_setup.md` — initial scaffold confirmed.
- `002_camera_fgs_poc.md` — camera-FGS frame-logger probe (CI green, `00539b8`).
- `002b_onscreen_frame_counter.md` — on-screen/notification frame counter for an
  adb-free survival test.
