# Task: CNN gaze — wire as a selectable signal source

## Goal

Add a third signal source, `SOURCE_CNN`, that runs the side-loaded model on the preprocessed eye
patches and feeds the **same** calibration + point-of-gaze as Iris/Blendshape — behind a toggle,
falling back to Iris when no model is loaded.

## Scope

Implement only the wiring: deliver the frame bitmap to the result handler, run preprocess →
`GazeCnn` inference when `SOURCE_CNN` is selected and a model is available, produce the eye-local
gaze from the model output, and add the UI toggle. Reuse the existing downstream unchanged.

## Context

`GazeCnn` (040) loads the model; `GazePreprocessor` (041) builds the `[1,36,60,1]` input. The model
outputs `[1,2]` `(pitch, yaw)` radians per eye. The existing pipeline already supports `SOURCE_IRIS`
and `SOURCE_BLENDSHAPE` via `SessionConfig.signalSource`; the CNN reuses the eye-local fields
(`irisXLocal`/`irisYLocal` ← `yaw`/`pitch`), so `binocularGaze` → calibration → point-of-gaze →
CSV all work unchanged. The MediaPipe LIVE_STREAM result callback can return the input image, so
the upright bitmap can be delivered alongside the `FaceLandmarkerResult` for eye cropping.

## Required changes

1. Make the face-helper result callback also deliver the **upright bitmap** that produced the
   result (from the MediaPipe input image / the bitmap submitted), so the service can crop eyes at
   result time.
2. Add `SessionConfig.SOURCE_CNN`. When it is selected **and** `GazeCnn.isAvailable`, run
   `GazePreprocessor` + `GazeCnn` per eye, map each output `(pitch, yaw)` into an `EyeFeature`'s
   eye-local fields, and produce the frame result that feeds the existing calibration / point-of-
   gaze / CSV. When the model is absent or inference fails, **fall back to Iris** for that frame.
3. Add the UI toggle (a third state alongside Iris/Blendshape) and record the active source in the
   session metadata (`signal_source` already exists). Persist the selection like the other modes.

## Do not implement

Do not:
- change the calibration model, the One-Euro filter, or the CSV schema;
- add the benchmark or docs (prompt 043);
- commit a model or add a build-time model download;
- block recording when no CNN model is present (just fall back to Iris).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- selecting CNN with a side-loaded model routes gaze through the model into the existing
  calibration/point-of-gaze; with no model, the app falls back to Iris and behaves exactly as
  before; the toggle and `signal_source` metadata reflect the choice.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; the only later queued prompt is `043` (benchmark/validation/docs). If the
signal-source API or the bitmap-delivery change affects what `043` must benchmark/document, update
`043` in this same commit and note it under "Scope deviations"; otherwise say nothing downstream is
affected. Do not edit already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`042_cnn_signal_source.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
