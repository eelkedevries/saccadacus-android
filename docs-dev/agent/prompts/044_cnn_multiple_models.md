# Task: CNN gaze — multiple side-loaded models

## Goal

Let the user side-load **several** gaze models, pick the active one in the app, and have each
recording labelled by model — so models can be A/B/C-compared by calibration error and latency.

## Scope

Implement only multi-model discovery, selection (persisted), and labelling. Do NOT change the model
contract, preprocessing, inference, or the calibration/point-of-gaze path. Fully additive; no model
is committed.

## Context

The CNN scaffolding (040–043) loads a single fixed `gaze_model.tflite`. To compare models the user
currently has to swap that one file. Each session's `meta_<stamp>.csv` already records
`calibration_error` and the benchmark sidecar records CNN latency, so labelling each recording with
the model name turns those into a per-model comparison.

## Required changes

1. `GazeCnn`: discover every `*.tflite` in a `gaze_models/` subdirectory of the app's external files
   dir (keep the legacy single `gaze_model.tflite` in the root working too) via an
   `availableModels(context): List<String>`; `load(context, name)` loads the named model (falling
   back to the first available when the name is empty/missing); expose the active model name for
   labelling. `isAvailable` / `infer` / `close` keep working.
2. Add a persisted `SessionConfig.gazeModelName` (saved/loaded by `AppSettings`, like the other
   modes). The service loads the selected model at session start.
3. Record the active model name in the session metadata (e.g. `gaze_model` in the meta sidecar),
   next to `signal_source`.
4. UI: when the gaze source is `cnn`, show a "Model: <name> (tap to switch)" control that cycles
   through the discovered models (mirroring the gaze-source toggle); show a clear hint when no
   models are present.

## Do not implement

Do not:
- change the model contract, preprocessing, inference, calibration, or CSV time-series columns;
- commit any model or add a build-time download;
- auto-evaluate/score models (the user compares via the recorded calibration error + latency).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- multiple `.tflite` files in `gaze_models/` are discovered and selectable, the choice persists,
  the selected model loads and drives gaze, and the active model name is recorded in the meta so
  recordings are comparable; with no models present, CNN still falls back to iris.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`044`); there are no later
prompts unless earlier execution added some. If a `045+` exists when this runs, apply the same
check to it. Otherwise say nothing downstream is affected. Do not edit already-run prompts, do not
renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`044_cnn_multiple_models.md`) as the commit message, then push. Do not commit partial
work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
