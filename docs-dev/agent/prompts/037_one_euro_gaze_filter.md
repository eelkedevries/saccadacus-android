# Task: One-Euro filter for gaze stability

## Goal

Stabilise the live point-of-gaze with a **1€ (One-Euro) filter** — a speed-adaptive low-pass
that smooths the jittery gaze dot during fixations without lagging fast eye movements.

## Scope

Implement only the work described in this prompt. It adds a One-Euro filter on the binocular
gaze signal that feeds the point-of-gaze. It does NOT change the per-eye eye-local CSV columns,
the existing opt-in eye-local EMA (prompt 021), calibration, or event detection.

## Context

Research (1€ Filter, Casiez et al., CHI 2012; verified as the canonical lowest-risk stability
quick win for noisy interactive signals) shows a first-order low-pass with a velocity-adaptive
cutoff beats a fixed EMA: at low gaze velocity (fixations) it uses a low cutoff (heavy
smoothing), and at high velocity (saccades) it raises the cutoff (stays responsive). The app's
point-of-gaze dot is visibly jittery; the existing eye-local EMA (021) is fixed-rate, off by
default, and smooths a different signal. A One-Euro filter on the gaze that feeds the
point-of-gaze is the direct, low-risk fix.

## Required changes

1. Add a small, time-aware `OneEuroFilter` (per-axis: `minCutoff`, `beta`, `dCutoff` constants;
   a `reset()`; a `filter(value, tNanos)`), plus a 2-D gaze wrapper. Standard formulation: a
   low-pass on the derivative sets the adaptive cutoff `minCutoff + beta·|dx|`, then a low-pass
   on the value; the first sample initialises and returns unchanged.
2. In the camera service, apply it to the **binocular gaze** (`binocularGaze(frame)`) using the
   frame's canonical timestamp **before** mapping to the point-of-gaze, so the dot, `GazeStats`
   and the `gaze_screen_x/y` CSV columns use the smoothed gaze. Reset the filter at session
   start and whenever gaze is unavailable (no face / blink) so it re-initialises cleanly. Leave
   the raw per-eye eye-local columns untouched. The filter is always on (One-Euro is safe by
   design; raw eye-local stays in the CSV for re-derivation) — no new UI/setting.
3. Add unit tests: first sample passes through; a constant input settles to that constant; a
   high-frequency jitter around a mean has its variance reduced; a sustained step is eventually
   followed (responsiveness preserved).

## Do not implement

Do not:
- change or remove the eye-local EMA (prompt 021) or its toggle;
- smooth the raw per-eye eye-local CSV columns or the recorded session samples;
- add a high-frequency capture mode, head-pose compensation, or a new ML model;
- add new settings/DataStore keys or UI controls for the filter.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- the point-of-gaze (`GazeStats` + `gaze_screen_x/y`) is produced from the One-Euro-smoothed
  gaze, verified by the filter unit tests (jitter reduced, step still followed).

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. The only later queued prompt is `038` (polynomial calibration), which
changes the calibration *model*; calibration capture reads `binocularGaze` directly (the raw
signal), not this service-side smoothed path, so this prompt does not affect it. While executing
this prompt, watch for anything that departs from the plan; after the required changes are done
and **before committing**, if a later not-yet-run prompt is affected, edit it in this same commit
and note it under "Scope deviations"; otherwise say nothing downstream is affected. Do not edit
already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`037_one_euro_gaze_filter.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
