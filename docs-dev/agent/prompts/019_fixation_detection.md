# Task: Fixation detection

## Goal

Detect fixations (stable-gaze intervals between saccades) and emit them as `event` rows.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a new detector plus its event rows and a live count — no schema change, and no change to saccade/blink logic.

## Context

`specification.md` §Domain rules defines saccade and blink detection; **fixations — the periods of stable gaze between saccades, the most basic eye-movement measure — are not detected yet**. `EventDetection.kt` holds `SaccadeDetector`/`BlinkDetector`; `CsvSessionWriter` has `appendSaccade`/`appendBlink` and the generic event columns including `event_type`. Use `event_type = "fixation"` — the combined-CSV column set is unchanged.

## Required changes

1. Add a `FixationDetector` over the same per-sample eye-local stream used for saccades: a contiguous run whose velocity stays below the saccade onset speed and whose dispersion stays small, lasting at least a minimum duration (e.g. 100 ms), is a fixation. Produce events with onset/offset/duration, a centroid, and a confidence from mean reliability.
2. Write fixation events via a new `CsvSessionWriter.appendFixation(...)` (`row_type = event`, `event_type = "fixation"`, reusing existing columns); finalise them at stop alongside saccades and blinks.
3. Surface a live fixation count in the events UI (`EventStats` / readout).

## Do not implement

Do not implement:
- changes to saccade/blink detection or thresholds;
- new CSV columns or gaze mapping;
- smooth-pursuit or micro-saccade classification.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- a recording yields `fixation` event rows interleaved with saccades, with plausible durations, and a live fixation count is shown.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `019`; still to run: `020`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected (e.g. `020` summarises fixations, `028` tests the detector); if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`019_fixation_detection.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
