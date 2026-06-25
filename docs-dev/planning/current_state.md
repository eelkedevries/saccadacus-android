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
  Built green in CI; **on-device frame survival CONFIRMED on one device (2026-06-24):
  frames kept arriving while the app was backgrounded / screen locked.**
- **Full v1 tracking pipeline (prompts 003–018)** — the `camera` service owns a MediaPipe
  Face Landmarker; per frame it derives eye-local/iris/head-pose/blink signals, detects
  saccades/blinks, records an in-memory session + motion sensors, and writes the combined
  CSV incrementally. Controls are Start/Pause/Resume/Mark/Stop (notification + UI). Extras:
  on-device benchmark + quality profiles, a low-light/face-lost alert, a live face/iris
  overlay, a sessions screen (save-to-Downloads / share / delete), session naming + notes,
  optional consent-gated raw video, and a frame-stall watchdog that re-acquires the camera.
- Compose UI: a scrollable control screen plus a sessions screen (no theming beyond the scaffold).

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
  v1 pipeline now exists.**
- **Post-010 fixes (on `main`, CI-green):** camera frames are now rotated upright before
  MediaPipe inference (`FaceLandmarkerHelper`) — a device test had shown a constant
  `head_roll ≈ -90°` from the frame being processed in raw sensor orientation, which also
  scrambled the eye-corner projection; the fix took roll to ~0°. Also added a **"Save CSV
  to Downloads"** action (MediaStore) so export no longer needs the share-sheet.
- **On-device gaze precision is PARKED for a daylight retest.** Detection, the full
  record→CSV→export pipeline, background survival, and the rotation fix are all confirmed
  working on-device; the open question is iris-gaze precision, which two night-time
  recordings showed as near-zero signal — most likely low light (MediaPipe iris collapses
  toward the eye centre in poor light), not a code bug. The `SignConvention` /
  `FaceMeshIndices` flags remain ready for one-line calibration once a good-light recording
  exists; the overlay (prompt 015) is the tool that will make that retest immediate.
- **Prompt queue `011`–`018` is implemented and CI-green** (executed 2026-06-24, committed
  directly to `main`): notification pause/resume (011) and interaction markers (012)
  complete the defined v1 controls; long-run survival — battery-opt exemption + camera
  re-acquire (013) — and SDK/AGP version verification (014) harden it; the live face/iris
  overlay (015), low-light/face-lost alert (016), sessions screen (017), and session naming
  + notes (018) round it out. Each prompt carried a "Cross-prompt impact check"; none
  invalidated a later prompt. **With this, the written v1 spec (§Scope) is fully implemented.**
- **Beyond-v1 queue `019`–`028` is implemented and CI-green** (executed 2026-06-25,
  directly to `main`): fixation detection (019), per-session summary stats (020), optional
  eye-local filtering (021), orphaned-`.tmp` recovery (022), orientation robustness (023),
  resource guards — storage/thermal (024), settings persistence via DataStore (025),
  onboarding + identity (026), user docs (027), and expanded unit tests (028). Each carried
  a "Cross-prompt impact check"; none invalidated a later prompt. One fix-forward was needed
  (025's `Settings` object clashed with `android.provider.Settings` → renamed `AppSettings`).
- **Gaze/calibration queue `029`–`033` is implemented and CI-green** (executed 2026-06-25,
  directly to `main`): spec → v0.3 scoping in calibration + blendshape source + point-of-gaze
  (029); an eye-look-**blendshape** gaze source selectable vs iris (030, the likely low-light
  fix); calibration capture + least-squares affine fit with unit tests (031); apply →
  point-of-gaze + `gaze_screen_x/y` CSV columns + overlay gaze dot (032); and held-out
  calibration validation + status (033). Each carried a "Cross-prompt impact check"; none
  invalidated a later prompt. **Honest caveat:** all of this compiles and is math-tested, but
  on-device gaze *accuracy* still needs the daylight retest — 030 is the most direct attempt
  to make the signal work in poor light, and calibration retires the manual `SignConvention`
  guessing. The yellow overlay dot + the on-screen calibration check error make that retest
  self-evaluating.
- **Daylight retest done + blink fix `034`–`035` (CI-green, 2026-06-25).** A good-light
  on-device recording confirmed the night-time near-zero gaze was indeed **low light**: the iris
  signal varies properly, reliability is 1.0, head-roll sits at ~0° (rotation fix holds), 5 clean
  fixations were detected, and **calibration produced a full-screen point-of-gaze**. The one real
  bug it exposed — blink mislabelling (≈58 % of *open* frames read "closing" because MediaPipe's
  `eyeBlink` open baseline is person/camera-angle dependent, then chained into multi-second false
  blinks) — is fixed: `034` makes per-eye blink classification adaptive to each eye's open
  baseline (`BlinkClassifier`); `035` caps a blink at ~800 ms (a longer closure is eyes-closed /
  look-away, not a blink) and takes the spec to v0.4. Gaze **accuracy** (does the dot land where
  you look) still depends on the calibration validation error — read it from the next `meta_*.csv`.
- **Full session bundle now exports to Downloads `036` (CI-green, 2026-06-25).** "Save to
  Downloads" previously copied only the combined CSV, stranding the sidecars (including `meta`,
  which holds the calibration validation error) in app-private storage. `036` unifies every
  per-session file onto the one session stamp (`sensors`/`frame_log`/`benchmark`/`video` joined
  `meta`/`summary`) and exports the whole stamp-matched bundle in one tap. The calibration error
  is also shown live on the calibration screen ("Mean check error … (screen units)").
- **Gaze quality/stability research + quick wins `037`–`038` (CI-green, 2026-06-25).** A
  deep-research pass (21/25 claims adversarially verified across primary sources — Google/Nature
  2020, FAZE, He/SAGE, TabletGaze, the 1€ Filter) identified the two highest-leverage, lowest-risk
  levers for this exact on-device pipeline: **temporal filtering** and **calibration beyond a single
  affine map**. Both shipped: `037` adds a **One-Euro** speed-adaptive filter on the point-of-gaze
  (smooths the dot during fixations without lagging saccades, always on); `038` upgrades calibration
  to a **2nd-order polynomial** map (affine fallback, 9-point fit grid + 4 held-out checks,
  backward-compatible persistence) → spec v0.5. A measured on-device data point preceding this:
  `meta` reported `calibration_error ≈ 0.099` (~a tenth of the screen, ~1-1.5 cm) with the affine
  map; the polynomial map targets that. Blink counts and durations are now physiological on-device.
- **Robust calibration `039` (CI-green, 2026-06-25).** A follow-up on-device run (on the slow
  **Battery** profile, ~6 fps) showed the bare polynomial **overfit** — held-out error rose
  0.099 → 0.112 and curvature amplified gaze noise into screen-space jumps. `039` makes calibration
  **select** the better of an affine and a **ridge-regularised** polynomial by held-out validation
  error (`GazeCalibrator.fitBest`), so it can never regress below affine; ridge penalises only the
  curvature terms, scaled to their own magnitude. Spec → v0.6. **Note for testing:** use the
  **Balanced/Quality** profile — Battery (~6 fps) undersamples saccades and adds calibration noise.
- **On-device gaze-CNN scaffolding `040`–`043` (CI-green, 2026-06-25).** A 3-agent scout confirmed
  the integration is clean but **no gaze model can be legally bundled** — every pretrained gaze model
  is trained on a non-commercial/research-only dataset (GazeCapture, MPIIGaze, ETH-XGaze, Gaze360),
  several explicitly forbidding "models trained on the dataset", and this repo is public. So the
  scaffolding is **additive and side-loaded** (user-supplied model, never committed): `040` standalone
  LiteRT (`org.tensorflow:tensorflow-lite`, CPU/XNNPACK, no Play Services; coexists with MediaPipe's
  embedded LiteRT — the spike) + a `GazeCnn` loader; `041` eye-crop/head-pose preprocessing (pure
  `GazeGeometry` + tests, Android `GazePreprocessor`); `042` a third `SOURCE_CNN` signal source that
  feeds the **same** calibration/point-of-gaze and falls back to iris when no model is present; `043`
  a CNN inference-latency benchmark + `docs/gaze_cnn.md`. Model contract `[1,36,60,1]` grayscale →
  `[1,2]` pitch/yaw. The app is now CNN-ready; a clean (self-trained) model drops in by placing
  `gaze_model.tflite` on the device. `044` extends this to **multiple side-loaded models**
  (`gaze_models/` dir, in-app selector, each recording labelled by `gaze_model` in the meta) so
  models can be A/B/C-compared by calibration error + latency. **Honest caveat:** research-only re-implementations score
  ~1.8-2.3 cm, so the current ~1 cm calibrated iris may still be the better signal until a
  good model is trained on clean data.
- **Deferred larger efforts (research-scoped; need explicit go-ahead):** **training a clean gaze
  model** for the now-built CNN scaffolding (MIT WebEyeTrack/BlazeGaze-style architecture trained on
  commercially-clean/self-collected data — needs compute + data outside CI); a **high-fps (≥120 Hz)
  capture mode** for saccade timing (the ~12 fps camera cadence is the verified binding constraint,
  but higher fps trades off against the low-light iris collapse); and **head-pose normalisation** of
  gaze (needs calibration spanning head poses). Plus: saccade/fixation
  threshold re-tuning, smooth-pursuit detection, gaze heatmaps, drift re-calibration, the empty
  `binocular_x/y_local` columns, and **release signing + R8/distribution** (keystore secret, not in a
  public repo; not exercised by `assembleDebug`).
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
- `011_notification_pause_resume.md` — Pause/Resume in the service + notification (CI green).
- `012_interaction_markers.md` — user "Mark" writes `task` rows to the CSV (CI green).
- `013_long_run_survival.md` — battery-opt exemption + camera-eviction re-acquire (CI green).
- `014_pin_sdk_agp_versions.md` — verified all build versions are stable GA, not preview (CI green).
- `015_face_iris_overlay.md` — live landmark/iris overlay for visual calibration (CI green).
- `016_low_light_quality_alert.md` — luma/face quality warning in UI + notification (CI green).
- `017_sessions_screen.md` — in-app sessions list: per-session save/share/delete (CI green).
- `018_session_naming_notes.md` — name + note per session, in filename + metadata (CI green).
- `019_fixation_detection.md` — fixation events (the missing core event type) (CI green).
- `020_session_summary_stats.md` — per-session summary + `summary_<stamp>.csv` sidecar (CI green).
- `021_eye_local_filtering.md` — optional EMA smoothing of eye-local, off by default (CI green).
- `022_orphaned_session_recovery.md` — finalise leftover `.tmp` sessions on launch (CI green).
- `023_orientation_robustness.md` — target-rotation follows the device mid-session (CI green).
- `024_resource_guards.md` — storage + thermal warnings and safe auto-stop (CI green).
- `025_settings_persistence.md` — DataStore-backed settings across launches (CI green; renamed
  `AppSettings` to avoid an `android.provider.Settings` clash).
- `026_onboarding_and_identity.md` — first-run explainer, permission rationale, brand palette (CI green).
- `027_user_docs.md` — `docs/` usage + CSV dictionary + privacy note (CI green).
- `028_unit_tests_expansion.md` — eye-local projection + fixation-detector JUnit tests (CI green).
- `029_spec_scope_gaze_calibration.md` — spec → v0.3: calibration, blendshape source, point-of-gaze in scope (CI green).
- `030_blendshape_gaze_source.md` — eye-look-blendshape gaze source, selectable vs iris (low-light fix) (CI green).
- `031_gaze_calibration_fit.md` — calibration capture + least-squares affine fit + unit tests (CI green).
- `032_apply_calibration_point_of_gaze.md` — apply calibration → `gaze_screen_x/y` CSV + overlay gaze dot (CI green).
- `033_calibration_validation.md` — held-out calibration validation error + on-screen status (CI green).
- `034_blink_state_adaptive_baseline.md` — per-eye adaptive open-baseline blink classifier; open eyes no longer read "closing" (CI green).
- `035_blink_event_duration_cap.md` — cap blink events at ~800 ms; long closures are not blinks; spec → v0.4 (CI green).
- `036_export_session_bundle.md` — Save-to-Downloads exports the whole session bundle; sidecars unified onto the session stamp (CI green).
- `037_one_euro_gaze_filter.md` — One-Euro speed-adaptive filter on the point-of-gaze for a stable gaze dot (CI green).
- `038_polynomial_calibration.md` — 2nd-order polynomial gaze→screen map with affine fallback + expanded targets; spec → v0.5 (CI green).
- `039_robust_calibration_selection.md` — calibration picks the better of affine / ridge-regularised polynomial by held-out error; never regresses below affine; spec → v0.6 (CI green; one test-threshold fix-forward).
- `040_cnn_runtime_model_loader.md` — standalone LiteRT/TFLite runtime + GazeCnn loader for a side-loaded model (dependency spike; coexists with MediaPipe) (CI green).
- `041_cnn_eye_preprocessing.md` — eye-crop/head-pose preprocessing: pure GazeGeometry (+ tests) + Android GazePreprocessor → `[1,36,60,1]` input (CI green).
- `042_cnn_signal_source.md` — third `SOURCE_CNN` gaze source feeding the existing calibration/point-of-gaze; iris fallback; 3-way UI toggle (CI green).
- `043_cnn_benchmark_docs.md` — CNN inference-latency benchmark + `docs/gaze_cnn.md` (model contract, side-load, A/B) (CI green).
- `044_cnn_multiple_models.md` — discover/select multiple side-loaded models (`gaze_models/`), persisted, each recording labelled by `gaze_model` for A/B/C comparison (CI green).
- `045_in_app_model_reference.md` — in-app collapsible "Gaze models — compare" fold-out (static `GazeModels` reference of 18 surfaced models: licence/year/accuracy/size/pros/cons) (CI green).
