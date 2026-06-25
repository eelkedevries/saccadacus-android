# Task: WebEyeTrack / BlazeGaze gaze profile

## Goal

Wire in the **WebEyeTrack / BlazeGaze** model (RedForestAI, MIT code, arXiv:2508.19544) as a new
multi-input gaze-CNN profile, so a side-loaded converted BlazeGaze model produces a point-of-gaze that
feeds the existing calibration → point-of-gaze pipeline.

## Scope

Add the `WEB_EYE_TRACK` profile end-to-end: multi-input inference in `GazeCnn`, the profile detection,
the preprocessing (pure geometry + an Android strip warp), the service wiring, and a conversion
recipe + docs. Side-loaded only — no weights committed. Default iris/blendshape tracking is untouched.

## Context

The shipped BlazeGaze model (verified against its `model.json`) takes three inputs — `image`
`[1,128,512,3]` RGB both-eyes strip (÷255), `head_vector` `[1,3]` unit head direction, `face_origin_3d`
`[1,3]` in cm — and outputs `[1,2]` a point-of-gaze in normalised screen `[-0.5,0.5]`. Its few-shot
MAML personalisation maps onto our existing 9-point calibration: the model's raw PoG becomes the gaze
signal and `GazeCalibrator` is the on-device personalisation. The eye strip is a homography warp of the
eye region; `head_vector` and `face_origin_3d` are derived from the MediaPipe landmarks + head pose
(both best-effort — we lack true camera intrinsics and the full metric face mesh).

## Required changes

1. `GazeCnn.inferMulti(inputs)` — run a multi-input model via `runForMultipleInputsOutputs`, returning
   the `[1,2]` output.
2. Extend `GazeModelProfile`/`GazeModelProfiles.detect` with `WEB_EYE_TRACK` (3 inputs: one
   `[…,128,512,3]` + two `[…,3]`), with a detection unit test.
3. Pure `WebEyeTrackGeometry` (unit-tested): the radial-padded eye-region quad, the `head_vector`
   (pitch'/yaw' reorder → Cartesian unit vector), and a best-effort metric `face_origin_3d`.
4. Android `WebEyeTrackPreprocessor.eyeStrip` — the two-stage homography warp → `128×512×3` RGB `[0,1]`.
5. Service wiring: when `activeProfile == WEB_EYE_TRACK`, build the 3 inputs, run `inferMulti`, and feed
   the binocular PoG through the existing per-eye → `binocularGaze` → calibration path; record latency.
6. Conversion recipe (Keras → TFLite) + docs; mark WebEyeTrack as wired in the catalogue.

## Do not implement

Do not:
- commit a model or add a build-time download;
- change the default iris/blendshape tracking, calibration maths, or CSV columns;
- add the Open Gaze / UniGaze profiles (later prompts).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- the `WEB_EYE_TRACK` profile is detected and unit-tested; the pure geometry is unit-tested; a
  side-loaded 3-input BlazeGaze model would be preprocessed + run + fed to calibration; docs describe
  the profile + the Keras→TFLite recipe + the approximation caveats.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Builds on `048` (profile abstraction). Follow-ups `050` (Open Gaze family) and
`051` (UniGaze) add further profiles/inference and must keep `inferMulti` + the profile `when` intact.
Do not edit already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`049_webeyetrack_profile.md`) as the commit message, then push. Do not commit partial
work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
