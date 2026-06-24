# Phase 0 — Grounded repository audit (Saccadacus Android stack research)

Companion to `android_stack_research_brief.md`. This is the **code-reading** phase:
verified facts from the two repositories, to be treated as ground truth by the
web-research phases and the final report. Where a fact could not be verified, it
is marked **UNVERIFIED**.

**Inspection date:** 2026-06-24.

**Method / caveats:**
- `saccadacus-android` was read from the **local checkout** (this working tree).
- `saccadacus` (browser) could **not** be cloned — the session's git egress is
  scoped to `eelkedevries/saccadacus-android`, so `git clone` of the browser repo
  returns HTTP 403 from the agent proxy, and the GitHub MCP tools are restricted to
  the Android repo. It was instead read file-by-file over `raw.githubusercontent.com`
  (branch `main`). The GitHub **commit SHA could not be retrieved** (the
  `api.github.com` tree endpoint was rate-limited, HTTP 429), so the browser-repo
  facts below are pinned to **branch `main` as of the inspection date**, not a SHA.
- `src/export/schema.ts` returned 404 over raw (path or name differs); its column
  set is taken from `PROPOSAL.md §14` instead and marked accordingly.

---

## A. `saccadacus-android` (the target repo)

Local checkout; scaffold commit `018e7e9` ("chore: scaffold … from eek-a-template").
Early **Kotlin + Jetpack Compose** scaffold, no app features yet.

**Build / SDK (`app/build.gradle.kts`):**
- `namespace`/`applicationId`: `com.example.saccadacusandroid` (placeholder).
- `compileSdk = 37`, `targetSdk = 36`, `minSdk = 24`, `versionName 0.1.0`.
- Java 17 toolchain; Compose enabled; `release` unminified (shrinking deferred).
- Plugins: `com.android.application` + `org.jetbrains.kotlin.plugin.compose`
  only — no `kotlin.android` (AGP 9 bundles the Kotlin plugin).

**Version catalogue (`gradle/libs.versions.toml`):**
- `agp 9.2.0`, `kotlin 2.3.10`, `coreKtx 1.17.0`, `activityCompose 1.12.4`,
  `lifecycleRuntimeKtx 2.11.0`, `composeBom 2026.06.00`; Compose UI + Material 3.
- Gradle wrapper committed; wrapper version **9.4.1** per the scaffold commit
  message (not separately re-verified — **UNVERIFIED**).

**Manifest (`app/src/main/AndroidManifest.xml`):**
- **No `<uses-permission>`, no `<uses-feature>`, no `<service>`.** A single
  launcher `MainActivity`. `allowBackup="true"` (note: should be revisited before
  any session data is stored on-device — see privacy).
- ⇒ Camera, foreground-service, notification, and sensor work is **entirely
  greenfield**.

**App code:** `MainActivity.kt` is a stub Compose "Hello" screen; `ui/theme/*`
default Material 3 theme. No ViewModel, DI, navigation, or service.

**CI (`.github/workflows/build-apk.yml`):** builds `assembleDebug` on
`ubuntu-latest` (JDK 17 temurin), uploads the `app-debug` artefact, validates
prompt numbering, runs a gitleaks scan. Triggers on push to all branches + PRs to
`main`. **No on-device/instrumented tests** in CI.

**Docs/process:** `docs-dev/` prompt-driven workflow (commit-to-`main`, one prompt =
one unit). `specification.md` and `current_state.md` are **empty stubs** — no
binding design decisions recorded yet, so this research is unconstrained by prior
canon.

**Version-reality flags for the web phase (do not assume):** confirm that
`compileSdk 37`, `AGP 9.2.0`, `Kotlin 2.3.10`, and `composeBom 2026.06.00`
actually resolve as of 2026-06-24, and that `minSdk 24` is compatible with (a) the
`camera` foreground-service type and its `FOREGROUND_SERVICE_CAMERA` permission
(declared/enforced on newer APIs) and (b) the minimum API of MediaPipe Tasks
Vision. These are the load-bearing toolchain questions for the web phase.

---

## B. `saccadacus` (the browser implementation) — primary evidence

**Stack:** React 19 + TypeScript 5.7 + Vite 6, Zustand 5 (UI state **only**),
`gl-matrix` 3.4 (linear algebra), uPlot (plots), Tailwind 4. Tests: Vitest (unit)
+ Playwright (e2e). Entry `src/main.tsx`. (`package.json`, `index.html`.)

**Tracking library:** **`@mediapipe/tasks-vision` ^0.10.35** — i.e. the **MediaPipe
Tasks Vision** Face Landmarker family, the same lineage available on Android.

### B.1 Maturity — the decisive finding
- `docs-dev/STATUS.md`: phases 1–9 all marked `done`.
- **But** `backendAdapters/README.md` states the directory is *"Empty until Phase 7,"*
  when *"a single throwaway MediaPipe Face Landmarker adapter is added for
  benchmarking."* And `STATUS.md`/AGENTS.md: the MediaPipe adapter is
  *"code-split and dev-gated behind `?spike=mediapipe`; it is not wired into the
  default boot path,"* with *"on-device benchmark numbers … pending [needing] a
  human to run them on the target devices before Phase 8 is confirmed."*
- The default backend through the built product is **`MockTrackingBackend.ts`**.

⇒ **The browser repo has NO production, validated tracker.** It is a *tested
algorithm + architecture scaffold* with a **throwaway MediaPipe spike** for
benchmarking. The real landmark→signal conversion (and any empirical eye-tracking
validity) **does not yet exist in either repo.** The Android project is therefore
porting *proven algorithms and a proven data model*, not a proven tracker — and
the hardest, unsolved part (landmark adapter, mirroring, true reliability, on-device
performance) is greenfield on both platforms.

### B.2 Source layout (`PROPOSAL.md §19`)
`camera/{cameraController,cameraConstraints,frameClock}` · `tracking/{TrackingBackend,
MockTrackingBackend, backendAdapters/}` · `signals/{eyeLocalCoordinates, headPose,
reliability, velocity, ringBuffer}` · `events/{detectSaccades, detectBlinks,
headMotionLabels}` · `tasks/{qualityCheck, followTheDots, gazeMapping}` ·
`export/{combinedCsv, schema}` · `workers/{trackingWorker, signalWorker, protocol}` ·
`visualisation/*` · `app/` + `state/uiStore.ts` (React/Zustand UI).

### B.3 Core interface (`src/tracking/TrackingBackend.ts`, verbatim types)
```ts
type VideoFrameLike = ImageBitmap | VideoFrame | HTMLCanvasElement;
interface TrackingBackendConfig { frameWidth: number; frameHeight: number; seed?: number }
type Selection = 'iris' | 'pupil';
type BlinkState = 'open' | 'closing' | 'closed' | 'opening' | 'unknown';
interface EyeFeatureResult {
  irisCentre?:  { xLocal: number; yLocal: number; reliability: number };
  pupilCentre?: { xLocal: number; yLocal: number; reliability: number };
  selected: Selection; selectedReliability: number; blinkState: BlinkState;
}
interface HeadPoseResult { yawDeg; pitchDeg; rollDeg; translationX?; translationY?; translationZ?; reliability }
interface TrackingFrameResult {
  pageTimestampMs: number; videoMediaTimeMs?: number; backendLatencyMs?: number;
  leftEye?: EyeFeatureResult; rightEye?: EyeFeatureResult; headPose?: HeadPoseResult;
  faceReliability: number; overlayLandmarks?: OverlayLandmarks;
}
interface TrackingBackend { initialise(config); processFrame(frame, pageTimestampMs); dispose() }
```
Notes: eye features are **already in eye-local coords** (`xLocal/yLocal`) in the
result — i.e. **the adapter does the projection and mirroring**. `pupilCentre` is
**optional** and **no model produces it** today (see B.6). `seed` exists for the
deterministic mock (useful as a parity fixture generator on Android).

### B.4 Eye-local projection (`src/signals/eyeLocalCoordinates.ts`, verbatim)
Pure function: `u` = unit vector left-corner→right-corner (positive `xLocal` =
participant's right), `v` = `u` rotated 90° (`vxn=-uyn, vyn=uxn`) so positive
`yLocal` = up; origin = corner midpoint; normalised by inter-corner distance
("eye-width units"); degenerate corners → `(0,0)`. **Mirroring/handedness is
explicitly delegated to the backend adapter** (the comment: *"A backend adapter
that receives image-space landmarks (y-down, mirrored) is responsible for
converting to this frame before calling, so this function stays pure and
sign-stable."*). No magic constants. ⇒ **clean line-by-line numeric port.**

### B.5 Event detectors (exact thresholds — parity-critical)
- `detectSaccades.ts`: onset speed **1.0**, offset speed **0.4** (eye-width
  units/s); duration **8–200 ms**; min amplitude **0.03** eye-width. Validates: no
  blink in interval; amplitude+duration; peak head angular velocity; mean
  reliability; binocular consistency weight **1.0** (consistent) / **0.6**
  (inconsistent). Confidence = `clamp(mean_reliability × consistency × head_motion_factor)`.
  Head-motion labels (`headMotionLabels`): `saccade_head_still` /
  `saccade_during_head_movement` / `uncertain_head_motion`.
- `detectBlinks.ts`: pure **state machine** over `BlinkState` — a contiguous run of
  `closing|closed|opening` bracketed by `open` is a blink; confidence **0.9** if the
  run reached `closed`, else **0.6**; `minDurationMs` default **0**. **Depends on the
  backend supplying `blinkState`** (e.g. from MediaPipe `eyeBlink` blendshapes).
⇒ **numeric ports with parity tests** keyed to these constants.

### B.6 Iris vs pupil (`PROPOSAL.md §4` + types)
Three modes: Automatic / Iris centre (default; *"more visible and stable than the
pupil"* on RGB) / Pupil centre (*"available … may perform well under favourable
lighting, high image quality, or future camera/tracking configurations"*). No
dedicated pupil model is present or specified; `pupilCentre` is an optional result
field. ⇒ On RGB selfie cameras + MediaPipe iris landmarks, the realistic signal is
**iris centre**; a **true pupil centre is not available** without a separate model.
Strong prior toward **deferring true-pupil from v1** (web phase to confirm against
MediaPipe docs + any open-source mobile pupil models).

### B.7 Storage / CSV (`PROPOSAL.md §14`; `schema.ts` itself was 404)
**Single combined CSV**, one row type per row (`row_type ∈
time_series | event | dot/task`), columns aligned on the canonical clock. Captured
column set includes: `timestamp_performance_now`, `video_or_frame_timestamp`,
`tracking_mode`, `eye_selection_mode`, `{left,right,binocular}_eye_{x,y}_local`,
`{left,right,iris,pupil}_reliability` (+ `*_eye_reliability`), `head_{yaw,pitch,roll}`,
`head_translation_{x,y,z}`, `blink_state`, `event_{type,onset,offset,duration,
direction,relative_amplitude,confidence,head_motion_label}`, `dot_{x,y,timestamp}`,
`gaze_{x,y}_mapped`, `gaze_mapping_{id,reliability}`. Raw video never exported;
optional local-only video. ⇒ **column names port directly**; only the
**timestamp columns** change (B.8) and the **write path** becomes incremental
Android file IO with locale-independent formatting.

### B.8 Timing model (`PROPOSAL.md §24–25`)
Canonical clock = **`performance.now()` ms**. Rule: the page stamps
`pageTimestampMs` at frame ingestion and **the backend echoes it back unchanged** —
*"the basis for all later alignment between time-series rows, event rows, and
dot-task rows."* Optional `videoMediaTimeMs` (from `requestVideoFrameCallback`);
`backendLatencyMs` = `processFrame` wall-clock. Frame pacing deliberately avoids
rVFC (Firefox rate-limiting), polling `performance.now()` with rAF fallback.
⇒ The **architecture ports directly**; on Android `performance.now()` →
`SystemClock.elapsedRealtimeNanos()`, plus a **wall-clock anchor** column for
external interpretation, and camera `SENSOR_TIMESTAMP` kept as the `video_or_frame_timestamp`.

---

## C. Per-module reuse classification (browser → Android)

| Browser module | Android disposition | Reason |
|---|---|---|
| `signals/eyeLocalCoordinates.ts` | **Line-by-line numeric port + parity tests** | Pure, portable math; sign/mirroring contract well-defined |
| `signals/reliability.ts` | Numeric port + parity tests | Pure aggregation (thresholds live in code — extract during port) |
| `signals/velocity.ts`, `ringBuffer.ts` | Direct port to Kotlin `FloatArray`/`DoubleArray` ring buffers | Already array-based, GC-conscious |
| `signals/headPose.ts` | Conceptual port | Recompute from MediaPipe facial-transform matrix + sensors |
| `events/detectSaccades.ts` | **Numeric port + parity tests** (1.0/0.4/8/200/0.03) | Exact thresholds must match across platforms |
| `events/detectBlinks.ts` | Numeric port + parity tests | State machine; needs backend `blinkState` |
| `events/headMotionLabels.ts` | Numeric port + parity tests | Still/moving thresholds |
| `tracking/TrackingBackend.ts` (types) | **Adapt** interface to Kotlin (`Image`/`Bitmap` for `VideoFrameLike`) | Keep the echo-timestamp contract |
| `tracking/MockTrackingBackend.ts` | Port (seeded) | Drives deterministic parity fixtures + UI dev without camera |
| `tracking/backendAdapters/*` (MediaPipe) | **Android-specific rewrite (greenfield)** | Only a throwaway web spike exists; landmark→eye/iris/pupil/head-pose + mirroring is the real new work |
| `camera/*` | Android-specific rewrite (CameraX/Camera2) | `getUserMedia` → Android camera in a **service** |
| `export/{combinedCsv,schema}.ts` | Conceptual port; **keep column names**, rewrite IO | Incremental, crash-safe, locale-independent writing |
| `workers/*` | Reference only → rewrite | Web Workers → coroutines/threads + channels |
| `tasks/*` (qualityCheck, followTheDots, gazeMapping) | Reference / **defer** | Gaze mapping is v2; needs stimulus coords |
| `visualisation/*`, `app/*`, `state/uiStore.ts` | Reference only → Compose + ViewModel rewrite | React/Zustand/Canvas → Compose |

---

## D. Parity hazards to carry into the port (flagged by the brief)
1. **Mirroring / eye labelling.** Front-camera image is mirrored and y-down; the
   browser keeps `projectEyeLocal` pure and pushes the mirror/handedness fix into
   the adapter. Android must reproduce **exactly that seam** (adapter converts to
   participant y-up frame; `xLocal+` = participant right, `yLocal+` = up) or every
   downstream sign flips.
2. **Timestamp units/domains.** `performance.now()` ms (double) → Android monotonic
   ns; the **echo rule** must survive the JNI/coroutine boundary; keep camera
   `SENSOR_TIMESTAMP` separately and record its `TIMESTAMP_SOURCE`.
3. **Event thresholds in eye-width units** (1.0/0.4/0.03) are resolution-independent
   by design — preserve the unit, don't reintroduce pixels.
4. **Blink depends on backend state** — Android adapter must emit a `BlinkState`
   (map from MediaPipe `eyeBlink_L/R` blendshapes) or blink detection yields nothing.
5. **`pupilCentre` is optional and unmodelled** — do not let an iris landmark be
   relabelled as pupil in the schema.

## E. Open gaps (could not verify in Phase 0)
- Browser repo **commit SHA** (api rate-limited) — facts pinned to `main`@2026-06-24.
- `export/schema.ts` exact formatting code (404 over raw) — column **names** known
  from the proposal; **decimal/locale formatting** must be read from code before the
  CSV parity port.
- Exact internals of `reliability.ts`, `headPose.ts`, `headMotionLabels.ts`,
  `velocity.ts` (not individually fetched) — interfaces/inputs known from the
  proposal; extract precise constants when porting each.
