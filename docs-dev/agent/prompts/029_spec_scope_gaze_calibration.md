# Task: Spec update — scope in gaze calibration, blendshape source, point-of-gaze

## Goal

Record the decision to bring gaze calibration, an eye-look-blendshape gaze source, and a
calibrated point-of-gaze into scope, by updating the binding spec and bumping its version.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a documentation/decision change to `docs-dev/reference/primary_authoritative/specification.md` (and `docs/csv_schema.md` if needed) — **no code**.

## Context

`specification.md` currently lists "gaze mapping / absolute point-of-gaze" and calibration as
out of scope for v1. We are now scoping them in (a v1.x extension), together with a more
robust **eye-look-blendshape** gaze source. Per `AGENTS.md`, a decision change must update the
spec and bump its version. Subsequent prompts (030–033) implement against this.

## Required changes

1. **§Scope** — add an in-scope v1.x line for calibration-based gaze and a calibrated,
   normalised point-of-gaze; remove/qualify the matching out-of-scope lines.
2. **§Domain rules** — add a *Signal sources* note (iris-centre, existing; **eye-look
   blendshapes** as an alternative, more robust in low light) and a *Calibration* note (an
   affine map fitted from on-screen targets producing a normalised point-of-gaze; the map
   subsumes `SignConvention`).
3. **§Data schemas** — append two columns `gaze_screen_x`, `gaze_screen_y` (normalised 0–1
   point-of-gaze, empty/NaN when uncalibrated) and document `tracking_mode ∈ {iris, blendshape}`.
4. Bump the spec version (0.2 → 0.3) with a one-line changelog entry.

## Do not implement

Do not implement:
- any code, UI, or CSV-writer change;
- a specific calibration algorithm beyond "affine fit from on-screen targets";
- new out-of-scope items.

## Acceptance criteria

The task is complete when:
- `specification.md` reflects the scope/domain/schema changes and the version is bumped;
- `./gradlew assembleDebug` still builds in CI (no code changed).

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `029`; still to run: `030`–`033`, plus any added later) implement against this spec. While executing this prompt, watch for anything that departs from the plan. After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`029_spec_scope_gaze_calibration.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
