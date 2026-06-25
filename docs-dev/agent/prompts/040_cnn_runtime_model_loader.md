# Task: CNN gaze — on-device runtime + model loader

## Goal

Add the standalone **LiteRT** runtime and a `GazeCnn` loader that loads a **side-loaded**
`.tflite` gaze model from the device (never committed to the repo) and reports availability. No
pipeline wiring or inference yet — this is the dependency/runtime spike.

## Scope

Implement only the work described in this prompt: the Gradle dependency, the loader/lifecycle,
and the documented model contract. Do NOT add preprocessing, wire a signal source, or change the
existing tracking. The default tracking path must be byte-for-byte unchanged.

## Context

Scouting (deep research, this session) concluded the integration is clean via standalone LiteRT
(`com.google.ai.edge.litert`, no Google Play Services — NNAPI is out), but **no gaze model can be
legally bundled** (all pretrained gaze weights are non-commercial/dataset-tainted, and this repo
is public). So the model is **side-loaded by the user** onto the device and never committed. The
**fixed model contract** the scaffolding targets: input a normalised **single-eye patch**
`[1, 36, 60, 1]` float32 grayscale in [0,1]; output `[1, 2]` float32 = `(pitch, yaw)` in radians.
A key risk: MediaPipe Tasks already embeds a LiteRT runtime, so the explicit LiteRT artifacts may
collide (duplicate classes / version skew) — this prompt exists to surface that in CI early.

## Required changes

1. Add the standalone LiteRT dependency (`com.google.ai.edge.litert` core + GPU) to the version
   catalogue and `app/build.gradle.kts`. Pin a version and rely on CI to confirm it resolves and
   coexists with `mediapipe-tasks-vision`; if there is a duplicate-class/version conflict, resolve
   it here (exclude/resolution strategy) — that is the point of this spike.
2. Add a `GazeCnn` object/class that, on demand, looks for a side-loaded model at a fixed path in
   the app's files dir (e.g. `getExternalFilesDir(null)/gaze_model.tflite`), loads it via LiteRT
   (CPU/XNNPACK by default, GPU optional with CPU fallback), exposes `isAvailable`, and closes
   cleanly. When the file is absent it is a no-op (`isAvailable == false`) — never throws.
3. Document the **model contract** (path, input `[1,36,60,1]` grayscale [0,1], output `[1,2]`
   pitch/yaw radians) and the side-load step in a dev doc; note the file is intentionally
   git-ignored and never committed.

## Do not implement

Do not:
- run inference, extract eye crops, or build the input tensor (prompt 041);
- add a `SOURCE_CNN` signal source or any UI (prompt 042);
- commit any `.tflite` gaze model or add a build-time download for one;
- change the existing iris/blendshape path, calibration, or CSV.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI with the LiteRT dependency resolved and coexisting with
  MediaPipe (no duplicate-class failure);
- `GazeCnn.isAvailable` is `false` when no model file is present (the default), and the app builds
  and runs unchanged.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI). If it fails on dependency resolution or a
duplicate-class conflict, fix it here before the prompt is considered done.

## Cross-prompt impact check

Prompts run in sequence; later queued prompts are `041` (preprocessing), `042` (wire SOURCE_CNN),
`043` (benchmark/docs). If the LiteRT artifact name/version or the model contract has to change
here, update the affected later prompt(s) in this same commit and note it under "Scope deviations".
While executing, watch for anything that departs from the plan; otherwise say nothing downstream is
affected. Do not edit already-run prompts, do not renumber, and do not expand this prompt's scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`040_cnn_runtime_model_loader.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
