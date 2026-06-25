# Task: Open Gaze / Google point-of-gaze profile

## Goal

Wire in the **Open Gaze / Google point-of-gaze** model family (the SAGE/iTracker-style recreation;
catalogue #7/#8/#9) as a new multi-input gaze-CNN profile `DUAL_EYE_POG`, so a side-loaded converted
model produces a point-of-gaze that feeds the existing calibration → point-of-gaze pipeline.

## Scope

Add the `DUAL_EYE_POG` profile end-to-end: profile detection, the dual RGB-eye-crop + eye-corner
preprocessing (pure geometry + Android crop/normalise), the service wiring (reusing `GazeCnn.inferMulti`
from 049), and a conversion recipe + docs. Side-loaded only — no weights committed. Default tracking
untouched.

## Context

The runnable reference (`DSSR2/gaze-track`) takes three inputs — `leftEye`/`rightEye` `[1,128,128,3]`
RGB eye crops (the left horizontally flipped; per-channel `(x/255-mean)/std`, mean
`(0.3741,0.4076,0.5425)`, std `(0.02,0.02,0.02)`) and `lms` `[1,8]` eye-corner coordinates — and outputs
`[1,2]` a point-of-gaze in **centimetres**. Calibration maps cm → screen (standing in for the model's
SVR/affine personalisation). **Honest note:** the named "Open Gaze" (arXiv:2308.13495) is **withdrawn**
with no code/weights; gaze-track is **unlicensed** + GazeCapture-tainted — so we ship only the *profile*
and the user must train/obtain weights.

## Required changes

1. Extend `GazeModelProfile`/`GazeModelProfiles.detect` with `DUAL_EYE_POG` (3 inputs: two
   `[…,128,128,3]` or `[…,3,128,128]` eye crops + one `[…,8]`), with a detection unit test.
2. Pure `OpenGazeGeometry` (unit-tested): the 8 eye-corner landmark coordinates and a square eye crop
   box.
3. Android `OpenGazePreprocessor.eyePatchRgb` — `128×128×3` RGB crop, optional horizontal flip,
   per-channel mean/std normalisation, NHWC output.
4. Service wiring: when `activeProfile == DUAL_EYE_POG`, build `[leftEye(flipped), rightEye, lms]`, run
   `inferMulti`, feed the binocular PoG through the existing per-eye → `binocularGaze` → calibration
   path; record latency.
5. Conversion recipe (PyTorch → ONNX → NHWC TFLite) + docs incl. the licensing reality; mark the
   catalogue row wired.

## Do not implement

Do not:
- commit a model or add a build-time download;
- change the default tracking, calibration maths, or CSV columns;
- add the UniGaze profile (next prompt).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- `DUAL_EYE_POG` is detected + unit-tested; the pure geometry is unit-tested; a side-loaded 3-input
  model would be preprocessed + run + fed to calibration; docs cover the profile, the recipe, and the
  licensing caveats.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Builds on `048`/`049` (profile abstraction + `inferMulti`). Follow-up `051`
(UniGaze) adds the full-face-normalised profile and must keep the profile `when` + `inferMulti` intact.
Do not edit already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`050_open_gaze_pog_profile.md`) as the commit message, then push. Do not commit partial
work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
