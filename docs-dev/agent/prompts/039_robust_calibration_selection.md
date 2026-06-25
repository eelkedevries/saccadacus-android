# Task: Robust calibration — model selection + regularisation

## Goal

Make calibration **never regress below affine**: fit both an affine and a (regularised) polynomial
map, and keep whichever has the lower **held-out** validation error. This fixes the observed
overfit where the polynomial amplified gaze noise and scored worse than affine on a noisy run.

## Scope

Implement only the work described in this prompt. It changes how the calibration *model is
chosen and regularised*; it does not change the basis, the capture flow, the point-of-gaze
application, or the One-Euro gaze filter.

## Context

On-device data (a low-fps, noisy run) showed the 2nd-order polynomial map (prompt 038) **overfit**:
held-out `calibration_error` rose 0.099 → 0.112 and tiny eye-signal jitter (~0.007) mapped to large
point-of-gaze jumps (0.05-0.08, max 0.43) — the polynomial's curvature terms amplified noise. The fix
is standard: (a) **model selection** — fit affine *and* polynomial, score both on the held-out
validation targets (prompt 033), keep the lower-error one, so the richer model can only ever help;
and (b) **ridge regularisation** of the polynomial's curvature terms (relative to their own scale,
so it is units-independent) to damp noise amplification.

## Required changes

1. Add a held-out **model-selection** fit: given the fit set and the validation set, build an affine
   candidate and (when there are enough points) a **ridge-regularised** polynomial candidate, score
   each by mean held-out distance on the validation set, and return the lower-error model together
   with that error. With no validation points, fall back to the existing plain fit.
2. Apply ridge only to the **curvature** terms (`gx·gy, gx², gy²`), scaled **relative** to those
   terms' own normal-equation magnitude (units-independent), so the affine part is unbiased and the
   penalty is robust across gaze scales. Leave the existing plain `fit(...)` (used by tests)
   unregularised.
3. Wire the calibration screen to use the model-selection fit, storing/displaying the chosen model's
   held-out error as before.
4. Add unit tests: on genuinely curved data the polynomial candidate is chosen (held-out error well
   below an affine-only fit); selection never returns a model worse than affine on held-out data;
   empty-validation and degenerate inputs are handled.
5. Update `specification.md` §Domain rules (calibration) to state the affine/polynomial choice is made
   by held-out validation with a regularised polynomial, and bump the spec version.

## Do not implement

Do not:
- change the polynomial basis, the calibration targets, or the capture/validation procedure;
- change the point-of-gaze path, the `gaze_screen_x/y` columns, or the One-Euro filter (037);
- add a gaze ML model, head-pose terms, or a high-fps capture mode.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- calibration fits both models and keeps the better-on-held-out one (polynomial when it genuinely
  helps, affine otherwise), with the polynomial's curvature ridge-regularised — covered by the unit
  tests;
- the spec calibration rule and version are updated.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`039`); there are no later prompts
unless earlier execution added some. If a `040+` exists when this runs, apply the same check to it.
While executing this prompt, watch for anything that departs from the plan; after the required changes
are done and **before committing**, if a later not-yet-run prompt exists and is affected, edit it in
this same commit and note it under "Scope deviations"; otherwise say nothing downstream is affected. Do
not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`039_robust_calibration_selection.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
