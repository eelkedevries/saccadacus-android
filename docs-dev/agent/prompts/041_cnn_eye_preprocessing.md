# Task: CNN gaze — eye-crop + head-pose preprocessing

## Goal

Turn a camera frame + MediaPipe landmarks into the model's input: a normalised single-eye patch.
Keep the geometry pure and unit-tested; isolate the Android bitmap sampling in a thin wrapper.

## Scope

Implement only the preprocessing that produces the `[1, 36, 60, 1]` grayscale [0,1] eye-patch
tensor from an upright bitmap + the eye landmarks + the 4×4 head-pose matrix. Do NOT run the model
or wire a signal source (prompt 042). The existing pipeline is unchanged.

## Context

The model contract (prompt 040) is a normalised single-eye patch. Appearance-based gaze uses the
eye-ring landmarks to bound the crop and the head-pose rotation (upper-left 3×3 of the facial
transformation matrix) for data normalisation. MediaPipe eye-ring/iris indices: right-eye corners
**33** (outer) / **133** (inner), iris centre **468**; left-eye corners **362** (inner) / **263**
(outer), iris centre **473**; eye-ring sets bound each eye. JVM unit tests cannot construct an
Android `Bitmap`, so the **crop geometry must be a pure function** (testable) and the pixel
sampling a thin, separate Android-only step.

## Required changes

1. Add a **pure** `GazeGeometry` that, from the normalised landmark coordinates (and image
   width/height), computes each eye's crop rectangle: bounding box of the eye-ring indices,
   expanded by a fixed margin and shaped to the 60×36 (W×H) aspect, clamped to the image. Optionally
   compute the in-plane roll from the head-pose 3×3 for upright alignment. Keep it free of Android
   types so it is unit-testable.
2. Add a thin `GazePreprocessor` (Android) that, given the upright `Bitmap` + landmarks + matrix,
   uses `GazeGeometry` to crop, resizes to 60×36, converts to grayscale [0,1], and fills the
   `[1,36,60,1]` float buffer — per eye. No inference here.
3. Add unit tests for `GazeGeometry`: the crop box matches expected pixel bounds for synthetic
   landmarks; the margin/aspect/clamping behave; degenerate/out-of-range landmarks are handled.

## Do not implement

Do not:
- run the model or touch `GazeCnn` (prompt 042);
- add a `SOURCE_CNN` signal source, UI, or change the existing tracking/CSV;
- depend on camera intrinsics (use the landmark-based crop + head-pose roll only).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- `GazeGeometry` produces correct, clamped crop boxes for the eye-ring landmarks, verified by the
  unit tests; `GazePreprocessor` builds the `[1,36,60,1]` tensor (compiles; not unit-tested as it
  needs a real Bitmap).

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; the later queued prompts are `042` (wire SOURCE_CNN) and `043`
(benchmark/docs). If the preprocessing input shape or `GazePreprocessor` API changes what `042`
must call, update `042` in this same commit and note it under "Scope deviations"; otherwise say
nothing downstream is affected. Do not edit already-run prompts, do not renumber, do not expand
scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`041_cnn_eye_preprocessing.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
