# Task: Apply calibration — point-of-gaze + CSV columns

## Goal

Apply the fitted calibration at runtime to produce a normalised point-of-gaze, record it in the
new CSV columns, and show a calibrated gaze dot on the overlay.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is the runtime application of an existing calibration, the two new CSV columns from the spec (prompt 029), and the overlay dot. It does not capture or fit a calibration (prompt 031) or validate it (prompt 033).

## Context

Prompt 031 produces and persists a `Calibration` (affine map). Spec (prompt 029) adds the
columns `gaze_screen_x`, `gaze_screen_y` (normalised 0–1, empty/NaN when uncalibrated). The
combined-CSV writer is `CsvSessionWriter` (append the new columns at the **end** of the header
so existing event/task/fixation row indices are unchanged). The overlay is prompt 015.

## Required changes

1. When a persisted calibration exists, compute `gaze_screen_x/y` per frame from the active
   gaze signal via the affine map; expose it via `SignalStats`.
2. Append `gaze_screen_x`, `gaze_screen_y` to `CsvSessionWriter.HEADER` (at the end) and
   populate them in the time-series rows when calibrated (empty otherwise); record calibration
   presence in the meta sidecar.
3. Draw a calibrated gaze dot on the overlay when a calibration is present.

## Do not implement

Do not implement:
- calibration capture/fit (prompt 031) or validation (prompt 033);
- reordering existing CSV columns (append only);
- gating recording on whether a calibration exists.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- with a calibration present, the CSV's `gaze_screen_x/y` are populated and the overlay shows a gaze dot; with none, those columns are empty and no dot is drawn.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `032`; still to run: `033`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from the plan. After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`032_apply_calibration_point_of_gaze.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
