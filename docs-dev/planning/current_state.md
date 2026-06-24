# Current state

Living, high-level orientation: what exists now, key decisions, and what is in
progress. Read it at session start. Records current reality; the binding canon is
`docs-dev/reference/primary_authoritative/specification.md` (the canon wins on conflict).

## Systems

- **Android scaffold only** — Kotlin + Jetpack Compose, single stub `MainActivity`,
  committed Gradle wrapper, CI **Build APK** workflow. No camera, service, sensors, or
  tracking code yet (manifest declares no permissions/services).

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

- **Next: prompt `002_camera_fgs_poc.md`** — the risk-first proof-of-feasibility
  (camera foreground service + CameraX front-camera frame logger). Its acceptance
  criterion (frames survive screen-off/app-switch for 30–60 min on real Pixel + Samsung
  devices) requires on-device testing by a human — CI only proves it builds.
- Remaining stages 003–010 are proposed in the research report §19; each will be drafted
  as a prompt when run.

## Prompts run

- `001_setup.md` — initial scaffold confirmed.
