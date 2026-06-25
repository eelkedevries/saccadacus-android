# Task: Polynomial calibration mapping

## Goal

Replace the single 3×3 **affine** gaze→screen map with a **2nd-order polynomial** map fitted from
more, better-spread calibration targets — the highest-leverage, low-risk accuracy upgrade — with
a safe affine fallback when there are too few points.

## Scope

Implement only the work described in this prompt. It generalises the calibration model, the fit,
and the calibration target set, with backward-compatible persistence. It does NOT add head-pose
compensation, a gaze ML model, continuous re-calibration, or a high-fps capture mode.

## Context

Verified research (Google/Nature 2020, FAZE, He/SAGE, TabletGaze — unanimous 3-0) identifies
per-user calibration as the single highest-leverage accuracy lever; for this geometry pipeline
the realizable form is a richer mapping than a single affine. A 2nd-order polynomial
(`[1, gx, gy, gx·gy, gx², gy²]`) is the textbook gaze-mapping upgrade: it captures the
perspective/curvature an affine map cannot. It needs ≥6 fit points, so the current 5 fit targets
must grow to a well-spread grid; the held-out validation error (prompt 033) already measures the
result.

## Required changes

1. Generalise `CalibrationModel` to hold 6 coefficients per axis over the basis
   `[1, gx, gy, gx·gy, gx², gy²]`; `map` evaluates the polynomial. Keep `map(gx, gy)`'s signature
   so callers are unchanged. Update `serialize`/`deserialize`, and make `deserialize`
   **backward-compatible** with the old 6-value affine string (load it as a polynomial with the
   higher-order coefficients zero) so a persisted calibration still loads.
2. Generalise `GazeCalibrator.fit` to solve the polynomial least-squares normal equations (via a
   general NxN linear solve with partial pivoting). **Fall back to the affine fit** (embedded in
   the polynomial model with higher-order coefficients zero) when there are fewer than 6 usable
   points or the system is singular/ill-conditioned, so calibration never regresses.
3. Increase the calibration targets to a well-spread 3×3 fit grid (9 points) plus a few interior
   **held-out** validation points, and keep the existing capture/validation/status flow.
4. Update the calibration unit tests: the fit recovers a known 2nd-order polynomial; it falls
   back to affine with <6 points; serialize→deserialize round-trips; a legacy 6-value affine
   string still deserialises.
5. Update `specification.md` §Domain rules (calibration) to describe the polynomial map with
   affine fallback, and bump the spec version.

## Do not implement

Do not:
- add head-pose terms, a gaze CNN, or continuous/implicit re-calibration;
- change the point-of-gaze application path or the `gaze_screen_x/y` columns;
- change the One-Euro gaze filter (prompt 037).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- calibration fits a 2nd-order polynomial from the expanded targets, falls back to affine with
  too few points, and still loads a previously-persisted affine calibration — all covered by the
  unit tests;
- the spec calibration rule and version are updated.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`038`); there are no later
prompts unless earlier execution added some. If a `039+` exists when this runs, apply the same
check to it. While executing this prompt, watch for anything that departs from the plan; after the
required changes are done and **before committing**, if a later not-yet-run prompt exists and is
affected, edit it in this same commit and note it under "Scope deviations"; otherwise say nothing
downstream is affected. Do not edit already-run prompts, do not renumber, and do not expand this
prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`038_polynomial_calibration.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
