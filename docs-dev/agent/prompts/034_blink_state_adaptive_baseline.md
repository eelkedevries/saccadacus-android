# Task: Adaptive blink-state baseline

## Goal

Classify per-eye blink state relative to an adaptive *open-eye* baseline so a normally open
eye is no longer mislabelled "closing".

## Scope

Implement only the work described in this prompt. This changes how a per-eye `eyeBlink`
blendshape score becomes a `BlinkState`. It does not change blink-event aggregation
(prompt 035), the CSV schema, the gaze pipeline, or the saccade/fixation detectors.

## Context

An on-device daylight recording shows MediaPipe's `eyeBlink` score for a fully *open* eye
sits well above zero (≈ 0.2–0.5, varying by person and camera angle — e.g. a phone held low
so the eyes look down at it). The current fixed mapping in `FaceSignalAdapter.blinkState`
(`>= 0.2` → `CLOSING`, `>= 0.5` → `CLOSED`) therefore marks most open frames as "closing"
(one 68 s recording: 58 % `CLOSING`, only 3 % `OPEN`, while the iris signal was healthy and
varying — i.e. the eyes were open). That mislabel feeds `isBlink` (fragmenting saccade /
fixation runs) and the blink detector (chaining into multi-second false blinks). The raw
scores are not in the CSV, so a fixed threshold cannot be retuned blind: classification must
be **relative to each eye's own open baseline**.

## Required changes

1. Add a small **stateful, per-eye** classifier that tracks each eye's *open* baseline (a
   slow EMA, updated **only while the eye reads open** so a blink cannot inflate it, clamped
   so the baseline cannot rise high enough to swallow a genuine blink) and maps a score to
   `OPEN` / `CLOSING` / `CLOSED` by how far it rises **above that baseline**. Warm up from the
   first score; expose a `reset()`.
2. Use it in `FaceSignalAdapter` for **both** the blendshape and iris paths, preserving the
   existing per-eye pairing and the `SignConvention` mirror/flip semantics exactly. Remove the
   old fixed-threshold `blinkState` mapping. Reset the classifier when the face is lost.
3. Add unit tests: a stable but elevated baseline reads `OPEN`; a clear rise reads
   `CLOSING` / `CLOSED`; a blink does not inflate the baseline (the next open frame still reads
   `OPEN`); `reset()` clears state.

## Do not implement

Do not:
- change `BlinkDetector` or blink-event aggregation (that is prompt 035);
- change the CSV schema, the gaze/calibration pipeline, or the saccade/fixation detectors;
- add per-eye blink UI or new settings.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- an open eye held at an elevated but stable score reads `OPEN`, while a genuine closure still
  reads `CLOSING` / `CLOSED`, verified by the new unit tests.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. The only later queued prompt is `035` (blink-event duration cap),
which bounds blink *duration* and is independent of how a single frame is classified — so this
prompt does not invalidate it (fixing classification reduces, but does not remove, the need for
035's safety cap). While executing this prompt, watch for anything that departs from the plan;
after the required changes are done and **before committing**, if a later not-yet-run prompt is
affected, edit it in this same commit and note it under "Scope deviations"; otherwise say
nothing downstream is affected. Do not edit already-run prompts, do not renumber, and do not
expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`034_blink_state_adaptive_baseline.md`) as the commit message, then push.
Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
