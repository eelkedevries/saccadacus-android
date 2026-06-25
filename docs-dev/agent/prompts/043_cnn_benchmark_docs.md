# Task: CNN gaze — benchmark, validation, and docs

## Goal

Make the CNN gaze source measurable and documented: a latency benchmark for the CNN path,
calibration validation that works with the CNN source (so it is A/B-comparable with Iris), and
user/dev docs covering the model contract and how to side-load a model.

## Scope

Implement only the measurement + documentation around the existing CNN source (040–042). Do NOT
change the model contract, preprocessing, or the signal-source wiring.

## Context

The CNN source (042) feeds the existing calibration + point-of-gaze, so the held-out calibration
validation error (prompt 033/039) already measures its accuracy — this prompt makes sure that path
works with `SOURCE_CNN` and surfaces the CNN's inference latency so a user can compare it against
Iris on their own device. The model is side-loaded and the file is never committed.

## Required changes

1. Add a CNN-path **latency** measurement (mean / p50 / p95 inference ms) surfaced where the
   existing benchmark stats are (the benchmark sidecar / readout), recorded only when the CNN
   source is active and a model is loaded.
2. Confirm the calibration capture + held-out validation works with `SOURCE_CNN` (the gaze comes
   from the model), so the on-screen "Mean check error" and `calibration_error` let the user A/B
   the CNN against Iris. Record the active `signal_source` with the result.
3. Write docs: a dev/user note covering the **model contract** (path, input `[1,36,60,1]` grayscale
   [0,1], output `[1,2]` pitch/yaw radians), how to place a `gaze_model.tflite` on the device, that
   it is never committed (git-ignored), and how to A/B CNN vs Iris via the calibration error.

## Do not implement

Do not:
- change the model contract, preprocessing, calibration, or signal-source wiring;
- commit a model or add a build-time download;
- add new CSV time-series columns.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- the CNN path reports an inference latency, calibration validation works with the CNN source, and
  the docs describe the model contract + side-load + A/B procedure.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`043`); there are no later
prompts unless earlier execution added some. If a `044+` exists when this runs, apply the same
check to it. Otherwise say nothing downstream is affected. Do not edit already-run prompts, do not
renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`043_cnn_benchmark_docs.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
