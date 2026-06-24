# Deep research prompt: Android software stack for Saccadacus background tracking

## How to run this brief (read first)

This brief has two phases, and they use different tools. **Do not collapse them.**

- **Phase 0 — Grounded repository audit (code-reading, not web search).** Read the
  two codebases directly from source. Produce a short, verified *audit brief* of
  established facts. This is a code-comprehension task; web search cannot do it well.
- **Phases 1–13 — Web research and architecture (live sourcing).** Conduct the
  technical investigation, treating the Phase 0 audit brief as settled ground truth
  rather than re-deriving it.

Reading a repository is **not** modifying it. You have read access to the Android
repository as a local checkout; read it freely. Do **not** edit, commit to, or open
pull requests against either repository.

## Priorities and effort allocation

Three questions are load-bearing — they decide the project. Give them maximum depth
and never compress them:

1. **Foreground-camera feasibility:** can a camera foreground service be *started
   while visible* and then *sustained* after the user switches apps, under current
   Android FGS, while-in-use, and background-activity-launch rules?
2. **True pupil vs iris:** is a distinct pupil-centre signal genuinely available
   on-device, or only an iris proxy — and does true pupil tracking therefore belong
   in v1 or get deferred?
3. **Timestamp and cross-app synchronisation model:** the canonical clock and the
   method for aligning Saccadacus with a task running in another app.

Everything else is secondary. The **staged implementation plan** and **open
decisions** sections are also primary deliverables — if output budget runs short,
compress prose in the sensors/perf/privacy sections, never the three questions above,
the staged plan, or the open decisions. Spread-thin uniform coverage is a failure
mode; proportional depth is the goal.

## Role

Act as a senior Android architect with expertise in:

* Android camera APIs and foreground services;
* on-device computer vision and edge machine learning;
* eye, iris, pupil, blink, and head-pose tracking;
* high-frequency sensor acquisition and timestamp synchronisation;
* scientific data collection, validation, and reproducibility.

Conduct a current, evidence-based technical investigation. Do **not** implement the
application. The goal is to select the most suitable Android software stack and define
a defensible architecture for implementation.

## Project objective

Saccadacus Android is a native Android application that records eye- and head-movement
data while the user interacts with another app.

Intended workflow:

1. The user opens Saccadacus.
2. The user selects a tracking mode and starts a session **while Saccadacus is
   visible**.
3. Saccadacus starts a user-visible camera foreground service.
4. The user switches to another app — browser, PDF reader, Obsidian, or experimental
   interface.
5. Front-camera frames continue to be processed **locally**.
6. A persistent notification provides pause, resume, and stop controls.
7. The user returns to Saccadacus to review and export the session.

The application must not start camera tracking silently or automatically. Android must
continue to show the normal camera-use indicator and foreground-service notification.

## Required outputs and signals

Record derived data including:

* left- and right-eye position;
* iris-based tracking signals;
* pupil-based tracking signals, where technically supportable;
* selectable left-eye, right-eye, binocular, and separate-eye outputs;
* blink events;
* saccade events and confidence values;
* head orientation and position;
* device orientation and relevant motion sensors;
* per-eye and per-signal reliability;
* tracking-loss periods and causes;
* camera and processing metadata;
* monotonic timestamps and wall-clock anchors;
* pause, resume, stop, task, and annotation events.

Raw camera footage must remain disabled by default; optional local raw-video recording
may be offered only after explicit user activation. Initial export should support CSV
and preserve sufficient metadata to interpret and synchronise the recording.

## Primary use cases

**Reading** — eye movements, blink rate, head stability, and tracking quality while
reading a PDF, webpage, or Obsidian note in another app.

**Experimental tasks** — run concurrently with a browser-based or native experiment
and align tracking data with stimulus/response data via timestamps or explicit
synchronisation events.

**Natural smartphone use** — record eye and head behaviour while the user moves
between apps during a defined observation session.

## Known boundary (preserve throughout)

Camera-relative eye movement does not by itself reveal which screen element the user is
viewing. Absolute screen gaze requires a gaze-mapping procedure and access to screen or
stimulus coordinates. Keep *camera-relative tracking*, *gaze mapping*, and *validated
point-of-gaze estimation* as three distinct concepts everywhere in the report; never
conflate them or overstate accuracy.

## Core research question

What is the best current software stack and application architecture for implementing
this product reliably on Android, given continuous front-camera use, local
computer-vision processing, the foreground-service lifecycle, scientific timestamping,
data export, privacy requirements, and reuse of relevant work from the browser
implementation?

Do not give a generic "Kotlin + CameraX + MediaPipe" answer. Compare credible
alternatives, verify platform constraints against primary sources, identify unresolved
technical risks, and justify every major recommendation.

---

# Phase 0 — Grounded repository audit

Read both repositories from source and produce a concise audit brief of **verified
facts** (each tagged with the file/commit it came from). Report the commit hash and
inspection date for each repository. This brief is an input to every later section;
later sections must not contradict it.

### Android project — read the local checkout
`https://github.com/eelkedevries/saccadacus-android`

Confirm actual state, not assumptions: module layout, `minSdk`/`targetSdk`/`compileSdk`,
the full dependency/version catalogue, CI workflow, documentation structure, and any
architectural constraints recorded in `docs-dev/`.

**Version reality check (do not skip):** the scaffold pins specific toolchain versions
(e.g. AGP, Kotlin, compile SDK, Compose BOM). Verify each pinned version **actually
resolves and exists** as of the research date, and that the chosen `minSdk` is
**compatible with** (a) the camera foreground-service types this product needs and (b)
the minimum API of every ML backend you recommend. Do not assume the pins are correct;
do not assume they are wrong. Check.

### Browser implementation — fetch and read the source
`https://github.com/eelkedevries/saccadacus`

This is a substantially developed TypeScript implementation and is **primary
evidence**. Read the actual source for at least: camera acquisition and frame timing;
`TrackingBackend` and the MediaPipe adapter abstractions; eye-local coordinate
calculations; iris and pupil signal handling; head-pose estimation; reliability
calculations; blink and saccade detection; head-motion labelling; worker/concurrency
design; ring buffers and velocity calculations; follow-the-dots and gaze mapping;
session state and timestamp handling; CSV schemas and export logic; tests and benchmark
documentation.

For each major module, classify it for Android as: **direct conceptual port /
line-by-line numerical port with parity tests / Android-specific rewrite / reference
only / unsuitable for reuse** — with the reason grounded in the code you actually read.
Flag coordinate conventions, eye labelling under mirrored front-camera input, timestamp
units, floating-point behaviour, and event thresholds explicitly, since these are the
parity hazards.

If any part of the browser repo cannot be accessed or read, **say so plainly** and mark
every downstream conclusion that depended on it as provisional. Do not infer the
contents of unread code.

---

# Investigation requirements (Phases 1–13)

## 1. Establish platform feasibility

Determine exactly what Android currently permits for camera processing after the user
switches to another app. Cover: supported Android versions/API levels; foreground-service
types and required manifest permissions; camera while-in-use permission semantics;
restrictions on **starting** a camera foreground service (including background-start /
BAL restrictions and the "must start while visible" requirement); behaviour after the
activity is stopped, destroyed, or removed from recents; process death and service
recreation; screen-off and lock-screen behaviour; notification requirements and action
controls; battery, thermal, Doze, and background-execution behaviour;
manufacturer-specific battery restrictions; camera conflicts when another app requests
the camera; implications for Google Play distribution and privacy declarations.

Separate clearly: (1) behaviour guaranteed by Android documentation; (2) behaviour that
depends on device/manufacturer; (3) behaviour that requires empirical testing; (4)
behaviour that is not technically or policy-compliantly supportable.

Using the Phase 0 audit, assess whether the repo's current
`minSdk`/`targetSdk`/`compileSdk` are appropriate and recommend exact values with
reasons, distinguishing stable from preview SDKs.

## 2. Compare camera acquisition stacks

Compare at least CameraX, Camera2, Camera2/ACamera via native C++, and any justified
hybrid. Evaluate: use from a **service** rather than only an activity; lifecycle
ownership and camera binding; front-camera selection and device compatibility;
previewless/off-screen image analysis; YUV access and conversion overhead; resolution
and frame-rate control; camera timestamps; backpressure and dropped-frame control;
exposure/focus/stabilisation/low-light behaviour; simultaneous image analysis and
optional video recording; thermal and power implications; testing support; maintenance
burden. Do not assume CameraX is automatically correct — give a reasoned choice and
define the conditions under which Camera2 would be required.

## 3. Compare computer-vision and machine-learning backends

Investigate current Android support, maintenance status, licences, model availability,
hardware acceleration, and practical suitability for at least: MediaPipe Tasks Vision
Face Landmarker; ML Kit face / face-mesh APIs; custom LiteRT/TensorFlow Lite models;
ONNX Runtime Mobile; ExecuTorch; OpenCV / native C++; credible open-source iris or
pupil-localisation models for mobile; and a hybrid pipeline combining face landmarks
with a dedicated pupil model.

For each candidate, determine whether it can provide or support: stable eye-corner
landmarks; iris centre; **actual pupil centre rather than an iris proxy**; eyelid
landmarks or blink measures; facial transformation matrices or sufficient landmarks for
head pose; per-frame confidence or useful inputs for custom reliability estimation;
live-stream processing; CPU/GPU/NNAPI or other Android acceleration; quantised models;
offline use; reproducible model versioning; acceptable app and model size; and a licence
compatible with a public repo and research distribution.

**Add an explicit Google Play Services axis.** Some backends (notably ML Kit face APIs)
historically depend on Google Play Services; others (e.g. MediaPipe Tasks) are
self-contained. Because this product targets research deployment **outside** the Play
Store and potentially on degoogled or locked-down devices, treat "runs with no Play
Services dependency" as a first-class comparison criterion, not a footnote.

**Pupil verdict (the decisive output of this section):** state definitively whether a
distinct pupil-centre signal is genuinely available from the recommended backend, or
whether only an iris landmark is. If a separate pupil model is required, describe the
most credible options, their computational cost, training/validation requirements, and a
clear recommendation on whether pupil tracking should be **deferred from v1**.

## 4. Implementation language and code-sharing strategy

Compare: native Kotlin for the whole pipeline; Kotlin with selected C++/NDK modules;
Kotlin Multiplatform for portable non-UI logic; retaining TypeScript algorithms in a JS
engine or WebView; compiling portable algorithms to WebAssembly; another approach only
when strongly justified. Evaluate each for performance, battery, numerical consistency,
testability, reuse of the existing TypeScript, maintenance, debugging, dependency risk,
and suitability for scientific software. Build on the Phase 0 per-module classification
rather than repeating it; give particular attention to mirrored-front-camera eye
labelling, timestamp units, floating-point behaviour, event thresholds, and
cross-platform reproducibility.

## 5. Runtime architecture

Develop and compare credible architectures for a service-owned tracking pipeline. The
recommended design must address: ownership of the camera and ML model; Compose-activity
↔ foreground-service interaction; pause/resume/stop and failure recovery; coroutines,
executors, threads, channels, Flow; bounded queues and latest-frame processing;
backpressure and intentional frame dropping; avoiding memory growth; isolation of
camera, inference, signal processing, event detection, storage, and UI; delivery of live
quality summaries to the UI; process death; notification actions; wake locks only if
genuinely needed; thermal adaptation and configurable sampling rates; safe shutdown and
file finalisation; recording diagnostics **without** storing raw images. Provide a
component diagram and a frame-to-export data-flow diagram (Mermaid acceptable).

## 6. Scientific timestamp and synchronisation strategy

Recommend a canonical timing model for all camera, inference, sensor, event,
notification, and session records. Compare camera image timestamps,
`SystemClock.elapsedRealtimeNanos()`, `SystemClock.uptimeMillis()` (or ns equivalents),
Unix/UTC wall-clock, sensor-event timestamps, and media-encoder timestamps when raw
video is enabled. The design must: use a monotonic canonical clock; preserve original
source timestamps; store wall-clock anchors for external interpretation; record frame
acquisition, inference completion, and write timing separately; quantify processing
latency and dropped frames; survive pause/resume; and document every clock-domain
conversion explicitly.

Investigate practical cross-app synchronisation: shared monotonic-device-clock
conventions; Unix-time anchors; explicit visual/auditory/interaction markers; deep links
or intents; local network or WebSocket markers; Accessibility or screen-capture
approaches only if relevant and policy-compliant; integration APIs for experiments
controlled by another app. **Recommend a minimal first-version method and a more precise
optional method.**

## 7. Storage, recording, and export

Compare append-only high-frequency sample storage; event and session metadata;
crash-resilient temporary files; Room/SQLite; flat binary; incrementally written CSV;
Protocol Buffers; Parquet/Arrow (only if realistic on Android); DataStore for config;
Storage Access Framework and MediaStore for export; optional raw-video via CameraX
VideoCapture, MediaCodec, or another approach. The recommendation must avoid holding an
entire long session in memory. Address sessions of minutes to hours; interrupted/crashed
sessions; atomic file finalisation; schema versioning; locale-independent number
formatting; missing data; tracking-loss intervals; metadata sidecars vs combined table;
file-size estimates at several sample rates; optional compression; privacy/deletion;
export compatibility with R, MATLAB, Python, and spreadsheets. Compare a single combined
CSV with separate sample/event/metadata files, recommend a first-version schema, and
explain any departure from the existing web export design (referencing the Phase 0
schema findings).

## 8. Device sensors and head/device pose

Determine which sensors to record and how to fuse or separate them: rotation-vector,
gyroscope, accelerometer, gravity, linear-acceleration, display rotation, camera-based
head pose, and whether ARCore adds anything useful without imposing unnecessary
constraints. Define coordinate frames and transformations for camera image, participant,
device, display, and world/gravity reference. Explain how device movement can be
distinguished from eye-in-head and head-relative-to-camera movement — and where the
available signals are insufficient.

## 9. UI, application architecture, and libraries

Assess whether the existing Kotlin + Jetpack Compose direction should be retained (treat
it as the default unless evidence overturns it). Compare only where relevant: Compose vs
Views; unidirectional data flow; ViewModel/lifecycle; bound vs started vs combined
service patterns; DI with Hilt, Koin, or none; navigation; permissions handling;
notification APIs; WorkManager for **post-session** work (not live capture); structured
logging; privacy-preserving crash reporting. Prefer the smallest maintainable stack; do
not add frameworks merely because they are common in enterprise Android.

## 10. Performance, battery, and thermal plan

Identify the principal computational costs: camera acquisition; image conversion/rotation;
face and eye inference; dedicated pupil inference (if applicable); head-pose calculation;
signal processing and event detection; sensor acquisition; storage; optional video
encoding.

Propose selectable operating profiles (e.g. quality / balanced / battery-saving) and,
for each, candidate resolution, target frame rate, inference cadence, and qualitative
trade-offs.

**No fabricated numbers.** Do not present any latency, fps, CPU/GPU, memory,
battery-drain, or temperature figure unless it is backed by a citable measurement, and
label such figures with their source and device. Where no measurement exists, say so and
instead specify the benchmark that would produce the number — do not fill the gap with
plausible-looking values. Then define an empirical benchmark protocol covering: inference
mean/median/p95/p99 latency; achieved analysed frame rate; dropped frames; end-to-end
latency; CPU/GPU/memory; battery drain; temperature and thermal throttling; tracking
reliability; service survival; one-hour and multi-hour sessions; optional-video overhead.
Include a representative low/mid/high device matrix across multiple Android versions, and
state which tests cannot be replaced by emulators.

## 11. Validation and scientific quality

Define how to validate that the Android port preserves the intended signals: numerical
parity tests against the browser algorithms using shared fixtures (built on the Phase 0
classification); synthetic landmark sequences; prerecorded, consented test videos;
coordinate and mirroring tests; blink and saccade event tests; timestamp and
dropped-frame tests; left/right eye identity tests; reliability calibration; head-motion
labelling tests; gaze-mapping validation if retained; cross-device and cross-orientation
comparisons. Distinguish software correctness from empirical validity of eye-movement
measurement. Identify which thresholds must remain configurable and which should be
estimated from data.

## 12. Privacy, security, ethics, and distribution

Assess camera and notification permission flows; explicit informed session start; clear
recording state; raw-video consent; local encryption options; Android backup exclusions;
exported-file exposure; deletion and retention; logs and crash reports; Play Store
data-safety declarations; research deployment outside the Play Store; and licence
compatibility of libraries and model assets. The app must not use covert recording,
accessibility abuse, or any attempt to hide camera activity.

## 13. Repository and migration plan

Map the recommended architecture onto the Android repository: Gradle modules/packages;
dependency-catalogue changes; service/camera/inference/signal/storage/export/UI
boundaries; model-asset placement; version pinning; reproducible builds; CI checks;
benchmark tooling; documentation and architectural decision records.

Then give a **staged implementation sequence** in which each stage is small enough to
become one reviewable Claude Code or Codex prompt, specifying: objective; files/modules
affected; dependencies introduced; acceptance criteria; tests or device checks; and
decisions that must already be settled. Earliest stages must reduce the highest technical
risks before substantial UI work, and must include (a) a proof-of-feasibility stage for
background front-camera processing on real devices and (b) a separate backend benchmark
stage.

---

# Required comparison tables

Provide at least these. Show **raw evidence separately from judgement**; never disguise
missing evidence as a precise score; where evidence is absent, write "no evidence found"
rather than guessing.

**A. End-to-end stack candidates** — compare at least three coherent combinations (not
individual libraries), e.g. CameraX + MediaPipe Tasks + Kotlin pipeline; CameraX/Camera2
+ face landmarks + dedicated pupil model; Camera2 + custom LiteRT/ONNX + Kotlin/C++.
Score with **declared weights** across: functional coverage; background-camera
suitability; tracking-quality potential; performance; battery/thermal; implementation
complexity; maintainability; device compatibility; **Play-Services-free distribution**;
open-source/licence suitability; reuse of existing Saccadacus work; scientific
reproducibility; project risk.

**B. Camera APIs** — CameraX vs Camera2 vs any justified native approach.

**C. Tracking backends** — face/iris/pupil/blink/head-pose options, including the
Play-Services-dependency and true-pupil-availability columns.

**D. Storage formats** — long-session storage and export options.

**E. Code-sharing strategies** — Kotlin port vs C++ core vs Kotlin Multiplatform vs
JavaScript/WebView reuse.

---

# Required final report structure (this is the canonical deliverable)

The investigation areas and tables above feed this report; do not reproduce them as three
parallel documents. Use exactly this structure:

1. **Executive recommendation**
2. **Feasibility verdict**
3. **Repository audit** (from Phase 0)
4. **Requirements and decision criteria**
5. **Android platform and foreground-camera constraints**
6. **End-to-end stack comparison**
7. **Recommended camera stack**
8. **Recommended tracking and ML stack**
9. **Pupil-tracking feasibility**
10. **Recommended runtime architecture**
11. **Code-reuse and migration analysis**
12. **Timestamp and cross-app synchronisation design**
13. **Storage, raw video, and export design**
14. **Sensors and coordinate systems**
15. **Performance, battery, and thermal strategy**
16. **Testing, benchmarking, and scientific validation**
17. **Privacy, security, distribution, and licences**
18. **Recommended repository structure**
19. **Staged implementation plan**
20. **Risks, rejected alternatives, and fallback options**
21. **Open decisions requiring human input**
22. **Source list**

The **executive recommendation** must state: the recommended primary stack; the
recommended fallback stack; what to reuse from the browser project; whether true
pupil-centre tracking belongs in v1; the minimum supported Android version; the
recommended target and compile SDKs; the largest unresolved technical risk; and the
first proof-of-feasibility experiment.

---

# Evidence standards

* Use information current on the research date; **record the research date explicitly**.
* Prefer primary sources: official Android docs and API references; AndroidX release
  notes and source; Google AI Edge, MediaPipe, ML Kit, LiteRT, ONNX Runtime, ExecuTorch
  docs; official model cards and licences; source code and issue trackers; peer-reviewed
  literature for measurement/validation claims. Use secondary sources only when primary
  evidence is unavailable or for real-device experience.
* Cite every material technical claim with a direct link, including publication or
  last-updated dates where relevant.
* Distinguish stable, beta, alpha, preview, deprecated, and abandoned technologies.
  Verify that referenced APIs and package versions actually exist.
* **Do not infer Android behaviour from desktop, web, or iOS implementations.**
* Distinguish four epistemic levels explicitly: *documented capability*, *reported
  behaviour*, *measured result*, and *your own inference*. **Never fabricate
  quantitative results** — an unmeasured number must be presented as an open benchmark,
  not a value.
* Flag contradictory evidence rather than silently choosing one source.
* Do not recommend paid cloud services for core tracking.
* Report licences for code, libraries, model files, and pretrained weights
  **separately**.
* Keep camera-relative tracking, gaze mapping, and validated point-of-gaze estimation
  distinct; do not overstate accuracy.

# Decision standard

Optimise for: (1) a technically feasible foreground-camera session; (2) reliable local
eye and head signal extraction; (3) scientifically interpretable timestamps and outputs;
(4) privacy-preserving local processing; (5) maintainability by a small project; (6)
reuse of valid Saccadacus concepts without preserving unsuitable web-specific
architecture; (7) progressive implementation with early real-device risk reduction.

Conclude with **one** clear recommendation. Do not end with several equally preferred
options unless a specific unresolved benchmark genuinely prevents a decision — in which
case name the exact benchmark, the decision threshold, and the fallback choice.
