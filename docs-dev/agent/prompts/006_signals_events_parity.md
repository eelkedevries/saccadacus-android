# Task: Signal buffers, saccade/blink/head-motion events, and parity tests

## Goal

Turn the per-frame signals into a buffered time series and detect saccade, blink, and
head-motion events using the browser algorithms' exact thresholds, verified by parity
tests.

## Scope

Signal buffering + event detection + JVM tests only. No persistence/export/UI beyond a
live event readout. This is the first prompt that adds **tests** (they are required here).

## Context

Builds on `005`. The thresholds and event shapes are fixed in `specification.md` §Domain
rules (saccade onset 1.0 / offset 0.4 / 8–200 ms / amplitude 0.03; blink state machine;
head-motion labels). Source algorithms + the seeded mock are catalogued in
`saccadacus_phase0_audit.md`.

## Required changes

1. Port pre-allocated ring buffers (Kotlin `FloatArray`/`DoubleArray`) for the continuous
   signals + timestamps, and finite-difference velocity.
2. Port `detectSaccades` (with the head-motion labelling) and `detectBlinks` (state
   machine) with the exact thresholds; keep thresholds configurable.
3. Add JVM unit/parity tests: feed shared synthetic fixtures (and the ported seeded mock)
   and assert event onsets/offsets/labels match expected values within tolerance. Wire
   the tests into the Gradle `test` task so CI runs them.
4. Show live saccade/blink/head-motion event counts on screen.

## Do not implement

Do not implement: the session model/recording, CSV export, tracking modes, gaze mapping,
or raw video.

## Acceptance criteria

- `./gradlew assembleDebug` builds and `./gradlew test` (or the unit-test task) passes in CI.
- Parity tests cover saccade thresholds, the blink state machine, and head-motion labels.
- On device: deliberate quick eye movements and blinks increment the event counts.

## Checks

Run `./gradlew assembleDebug --no-daemon` and the unit-test task (via CI).

## Commit and push

If the scope was followed and checks pass, commit on `main` using this file's exact
filename (`006_signals_events_parity.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
