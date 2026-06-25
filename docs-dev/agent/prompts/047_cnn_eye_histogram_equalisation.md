# Task: MPIIGaze-style eye normalisation (histogram equalisation)

## Goal

Bring the gaze-CNN eye-patch preprocessing one faithful step closer to the MPIIGaze appearance-based
recipe by **histogram-equalising** the grayscale eye patch (the contrast-normalisation step MPIIGaze
applies to its eye image, which also lifts low-light contrast — a known weak spot here), and document
how to obtain a model that matches our single-input contract, including the official-weights gap.

## Scope

Preprocessing parity + documentation only. The input tensor **shape stays `[1,36,60,1]`**; the only
change is that the grayscale values are histogram-equalised before normalisation to `[0,1]`. No change
to model loading/selection, the signal-source wiring, calibration, point-of-gaze, or any recording.

## Context

A source check of `ptgaze` (`hysts/pytorch_mpiigaze_demo`, MIT code) established that the official
MPIIGaze model is a **two-input** network (eye image + a 2-D *normalised* head-pose vector) that also
relies on a full data-normalisation warp (camera intrinsics + 3-D face model + `cv2.equalizeHist`),
with its output un-rotated by the per-frame normalising matrix. So the pretrained weights are **not** a
drop-in on our single-input contract (recorded in `docs/gaze_models.md`). The achievable, CI-verifiable
first increment toward MPIIGaze is the eye-image **histogram equalisation**: it is contract-preserving
(shape unchanged), matches MPIIGaze's eye-image contrast step, and helps the low-light case. The
faithful two-input + warp path (to reuse the official checkpoint) is a separate, larger follow-up.

## Required changes

1. Add a **pure** (Android-free, unit-testable) `GazeImageOps.equalizeHist(gray: IntArray): IntArray`
   that matches OpenCV `cv2.equalizeHist` (256-bin cumulative histogram, `v -> round((cdf[v] - cdfMin)
   / (N - cdfMin) * 255)`, a flat image returned unchanged), with unit tests (empty, flat, a known
   ramp, full-range endpoints, monotonic + in-range).
2. Apply it in `GazePreprocessor.eyePatch`: compute the Rec. 601 luma as an 8-bit grayscale buffer,
   histogram-equalise it, then normalise to `[0,1]`. The returned tensor shape is unchanged.
3. Update the documented contract to say the eye patch is **histogram-equalised** grayscale `[0,1]` in
   `docs/gaze_cnn.md`, `docs/gaze_models.md`, and the `GazeCnn` contract comment; add an "Obtaining an
   MPIIGaze-style model" note in `gaze_cnn.md` explaining the official two-input + warp gap and that the
   contract targets a single-input MPIIGaze-style eye CNN.

## Do not implement

Do not:
- add the faithful two-input / data-normalisation-warp / head-pose-input path (separate follow-up);
- change the input tensor shape, model loading/selection, calibration, or any recording;
- commit a model or add a build-time download; add no new CSV columns.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- the eye patch is histogram-equalised before normalisation (shape unchanged), the equaliser is
  unit-tested, and the contract docs say "histogram-equalised" with the MPIIGaze-model note.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`047`); there are no later prompts
unless earlier execution added some. It refines the eye-patch contract wording introduced by 040–043
and catalogued in 046 — keep those consistent (done here). If a `048+` exists when this runs, apply the
same check to it. Do not edit already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`047_cnn_eye_histogram_equalisation.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
