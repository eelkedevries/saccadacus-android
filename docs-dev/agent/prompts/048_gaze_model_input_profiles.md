# Task: Gaze model input-profile abstraction

## Goal

Make the side-loaded-gaze-CNN runtime aware of a model's **input profile** (the shape/number of input
tensors it expects), so different model families can be wired in without the app blindly feeding the
wrong inputs. Introduce the abstraction and detect today's single profile; gate the existing eye-gray
inference on it. This is the foundation for multi-input models (WebEyeTrack, etc.) wired in 049+.

## Scope

Add a pure `GazeModelProfile` + detection from the loaded model's input tensor shapes, expose the
detected profile on `GazeCnn`, and gate the existing eye-gray CNN path on `EYE_GRAY`. No new
preprocessing, no multi-input inference yet, no change to the default iris/blendshape tracking.

## Context

The CNN path (040–047) assumes one input `[1,36,60,1]`. Upcoming models (WebEyeTrack: an RGB eye strip
+ head-pose + origin → point-of-gaze; full-face models; multi-input PoG models) have different input
contracts. Rather than guess per model, detect the profile from the interpreter's input tensor shapes
at load. For now the only recognised profile is `EYE_GRAY`; an unrecognised model yields a null profile
and the CNN source falls back to iris (so a wrong-shaped side-loaded model can never be mis-fed).

## Required changes

1. Add a **pure** `GazeModelProfile` enum (start with `EYE_GRAY`) and a pure
   `GazeModelProfiles.detect(inputShapes: List<IntArray>): GazeModelProfile?` that matches the input
   tensor shapes (treat a leading batch dim of 1 as optional), with unit tests.
2. In `GazeCnn`, after loading a model read its input tensor shapes and store the detected
   `activeProfile` (reset on close); leave existing `infer` behaviour unchanged.
3. Gate the eye-gray CNN application (in the service) on `GazeCnn.activeProfile == EYE_GRAY` in addition
   to the existing source/availability checks, so only an eye-gray model is fed the eye-gray path.
4. Note the profile detection in `docs/gaze_cnn.md`.

## Do not implement

Do not:
- add multi-input inference or any new preprocessing/model family (that is 049+);
- change the model contract for `EYE_GRAY`, calibration, recordings, or the default tracking;
- commit a model or add a build-time download.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- `GazeModelProfiles.detect` is unit-tested; `GazeCnn` exposes the detected `activeProfile`; the eye-gray
  CNN path runs only when the profile is `EYE_GRAY`; `docs/gaze_cnn.md` mentions profile detection.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Follow-ups `049` (WebEyeTrack) and later (`050` Open Gaze, `051` UniGaze) will
extend the `GazeModelProfile` enum + detection and add multi-input inference. This prompt only adds the
abstraction + the `EYE_GRAY` profile, so it does not invalidate them; they build on it. Do not edit
already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`048_gaze_model_input_profiles.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
