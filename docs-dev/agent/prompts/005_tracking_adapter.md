# Task: Tracking backend interface and eye-local / iris / head-pose adapter

## Goal

Convert MediaPipe landmarks into the project's derived per-frame signals — eye-local iris
position, head pose, blink state, and reliability — behind a Kotlin `TrackingBackend`
interface, with front-camera mirroring resolved correctly.

## Scope

The landmark→signal adapter and its result types only. Event detection (saccades/blinks
over time) and recording are later prompts. This is the parity-critical stage.

## Context

Builds on `003`. The result types, the **eye-local frame convention**, the
**mirroring/handedness rule**, and the iris-vs-pupil rule are defined in
`specification.md` §Architecture/§Data schemas/§Domain rules — follow them exactly. Port
the shape of the browser repo's `TrackingBackend` (see `saccadacus_phase0_audit.md`).

## Required changes

1. Define Kotlin types mirroring the browser contract, adapted for Android (e.g. an
   `Image`/`Bitmap` frame input): `TrackingFrameResult` with per-eye `irisCentre`
   (`xLocal`/`yLocal`/reliability), `selected`/`selectedReliability`, `blinkState`, plus
   `headPose` (yaw/pitch/roll + translation) and `faceReliability`. `pupilCentre` stays
   defined but **unpopulated** (iris-centre only — spec §Domain rules).
2. Implement a `MediaPipeFaceBackend` that, per frame: takes the eye-corner and iris
   landmarks, **converts image-space (mirrored, y-down) to the participant frame**, then
   projects to eye-local coordinates (positive `xLocal` = participant right, positive
   `yLocal` = up); derives head pose from the transformation matrix; maps blink blendshapes
   to `blinkState`; and estimates per-signal reliability.
3. Show the live derived values on screen (left/right eye-local x/y, head yaw/pitch/roll,
   blink state, reliability).

## Do not implement

Do not implement: saccade/blink **event** detection over time (prompt 006), ring buffers,
the session model, export, modes, or raw video. Do not populate a pupil signal.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: eye-local x/y move in the correct direction (look right → `xLocal` increases
  toward participant-right; look up → `yLocal` increases); head yaw/pitch/roll track head
  rotation; blink state flips on blink.
- Mirroring/eye-identity is correct (left eye labelled left from the participant's view).

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI); verify sign/mirroring directions on device.

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`005_tracking_adapter.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
