# Task: Calibration validation and status

## Goal

Validate a calibration against held-out targets, show a calibration-quality readout, and allow
clearing / re-calibrating.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This reuses the capture (031) and apply (032) paths to measure error, plus a status UI and a clear/re-calibrate control. It does not change the fit model.

## Context

Prompt 031 captures targets and fits the affine map; prompt 032 applies it to produce a
point-of-gaze. A calibration is only useful with a known quality, so this adds a validation
pass and surfaces calibration state.

## Required changes

1. Capture a few **held-out** validation targets (not used in the fit), apply the calibration,
   and compute an error metric (mean and max normalised-screen distance) plus a simple
   quality label.
2. Show calibration status in the UI (calibrated / uncalibrated, last validation error) and
   record it in the session metadata.
3. Allow clearing the calibration and re-running calibration.

## Do not implement

Do not implement:
- changing the fit model or the capture procedure;
- blocking or altering recording based on calibration quality;
- new CSV time-series columns beyond those added in prompt 032.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- after calibrating, the UI shows a validation error / quality label and the calibration state; clearing and re-calibrating work.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`033`); there are no later prompts unless earlier execution added some. If a `034+` exists when this runs, apply the same check to it. While executing this prompt, watch for anything that departs from the plan; after the required changes are done and **before committing**, if a later not-yet-run prompt exists and is affected, edit it in this same commit and note it under "Scope deviations"; otherwise say nothing downstream is affected. Do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`033_calibration_validation.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
