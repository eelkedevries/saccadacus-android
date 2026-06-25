# Task: Bound blink-event duration

## Goal

Stop reporting non-physiological long eye-closures as blinks: a closure longer than a
physiological maximum is eyes-closed / look-away, not a blink event.

## Scope

Implement only the work described in this prompt. This adds an upper duration bound to
`BlinkDetector`. It does not change blink-state classification (prompt 034), the CSV schema, or
any other detector.

## Context

`BlinkDetector` aggregates a contiguous non-open run into a single blink event with no upper
bound, so a long eye-closure (or a residual mislabel) is reported as one absurd blink — an
on-device recording produced "blinks" of 9, 18 and 32 seconds. A spontaneous blink is
≈ 100–400 ms (a slow/voluntary blink up to ≈ 500 ms); anything beyond that is not a blink.

## Required changes

1. Add an upper duration bound to `BlinkDetector` (a named constant, ≈ 800 ms, comfortably
   above a real blink and below the spurious multi-second runs). A non-open run whose duration
   exceeds the bound is **not** emitted as a blink event. Keep the existing `minDurationMs`
   behaviour and the 0.9 / 0.6 confidence rule unchanged for in-range blinks.
2. Add a unit test: a multi-second closed run yields no blink event, while an in-range run
   (e.g. ≈ 300 ms) still yields exactly one. Confirm the existing `detectsBlinkWithHighConfidence`
   test (a 30 ms blink) still passes.
3. Update `specification.md` §Domain rules (blink) so the rule reads "a contiguous non-open run
   bracketed by open, **up to a maximum duration**, is a blink; a longer closure is treated as
   eyes-closed / look-away, not a blink", and bump the spec version.

## Do not implement

Do not:
- change blink-state classification or thresholds (that is prompt 034);
- emit a new row type or CSV column for the long-closure interval;
- change saccade / fixation detection.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- a multi-second closed run produces no blink event and an in-range run still produces one,
  verified by the new unit test;
- the spec's blink rule and version are updated.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`035`); there are no later
prompts unless earlier execution added some. If a `036+` exists when this runs, apply the same
check to it. While executing this prompt, watch for anything that departs from the plan; after
the required changes are done and **before committing**, if a later not-yet-run prompt exists
and is affected, edit it in this same commit and note it under "Scope deviations"; otherwise say
nothing downstream is affected. Do not edit already-run prompts, do not renumber, and do not
expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`035_blink_event_duration_cap.md`) as the commit message, then push.
Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
