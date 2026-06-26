# Task: UniGaze / full-face gaze profile

## Goal

Wire in **UniGaze** (and the full-face model family — ETH-XGaze / MobileGaze / L2CS-Net) as a new
single-input gaze-CNN profile `FULL_FACE`, so a side-loaded converted full-face model produces gaze
that feeds the existing calibration → point-of-gaze pipeline.

## Scope

Add the `FULL_FACE` profile end-to-end: profile detection, a face-crop preprocessing (pure geometry +
an Android crop/ImageNet-normalise), the service wiring (reusing the existing single-input
`GazeCnn.infer`), and a conversion recipe + docs. Side-loaded only — no weights committed. Default
tracking untouched.

## Context

UniGaze (ut-vision, WACV 2026) takes a single `[1,224,224,3]` RGB face crop and outputs `[1,2]`
pitch/yaw. Verified facts that shape this prompt: (a) the model is trained on an **ETH-XGaze-style
data-normalised** face (solvePnP head pose + `warpPerspective` + output un-rotation) — we use a plain
landmark face box instead, so accuracy is reduced vs the paper (the paper ablates this), partly
recovered by calibration; the faithful warp is a separate, larger follow-up. (b) Only **UniGaze-B** is
on-device-viable; **-L/-H (0.6–1.3 GB) will not run on a phone**. (c) Licence is **non-commercial**
(ModelGo MG-BY-NC, NC even after conversion); weights are research-only — never committed. The same
profile serves ETH-XGaze, MobileGaze and L2CS-Net (all full-face → angles).

## Required changes

1. Extend `GazeModelProfile`/`GazeModelProfiles.detect` with `FULL_FACE` (single `[…,224,224,3]`
   input), with a detection unit test.
2. Pure `FaceCropGeometry.faceCropBox` (square face bbox over the landmarks) with unit tests.
3. Android `FaceCropPreprocessor.facePatch` — `224×224×3` RGB crop, ImageNet per-channel normalisation,
   NHWC.
4. Service wiring: when `activeProfile == FULL_FACE`, build the face patch, run the existing
   single-input `GazeCnn.infer`, feed the `[1,2]` output through the per-eye → `binocularGaze` →
   calibration path; record latency.
5. Conversion recipe (PyTorch → ONNX → NHWC TFLite / ai-edge-torch) + docs incl. the data-norm-warp,
   size and licence caveats; mark the catalogue + unlock order.

## Do not implement

Do not:
- add the ETH-XGaze data-normalisation warp / un-rotation (a documented follow-up);
- commit a model or add a build-time download;
- change the default tracking, calibration maths, or CSV columns.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- `FULL_FACE` is detected + unit-tested; the pure crop geometry is unit-tested; a side-loaded
  `[1,224,224,3]` model would be preprocessed + run + fed to calibration; docs cover the profile, the
  recipe, and the (data-norm / size / licence) caveats.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Builds on `048` (profile abstraction). This is currently the last queued
prompt (`051`); if a `052+` exists when this runs, apply the same check to it. Do not edit already-run
prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`051_unigaze_full_face_profile.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
