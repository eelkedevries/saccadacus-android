# Task: Orientation robustness

## Goal

Keep the analysed frame upright when the device is rotated during a session.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is target-rotation tracking in the service — no change to the eye-local projection maths.

## Context

The upright-frame fix (`FaceLandmarkerHelper`) rotates the bitmap by `imageInfo.rotationDegrees`, which follows the `ImageAnalysis` target rotation. In a previewless foreground service the target rotation defaults to the bind-time display rotation and does not track device rotation, so rotating the phone mid-session can skew the face and break the eye-corner projection (which assumes an upright face).

## Required changes

1. Register an `OrientationEventListener` (or equivalent) in the service that maps the device's physical orientation to a `Surface` rotation and calls `ImageAnalysis.setTargetRotation(...)`, so `imageInfo.rotationDegrees` stays correct as the device rotates.
2. Ensure this composes correctly with the existing bitmap upright-rotation (exactly one upright rotation, no double-rotation).
3. Register on start and clean the listener up on stop.

## Do not implement

Do not implement:
- landscape-specific UI layout;
- changing the projection or sign conventions;
- heavy per-frame orientation work.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, rotating the phone during a session keeps `head_roll` near 0 and the overlay upright (device check).

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `023`; still to run: `024`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`023_orientation_robustness.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
