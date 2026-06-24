# Task: Live face/iris overlay

## Goal

Draw the tracked face landmarks (with the iris centres emphasised) on screen in real time, so tracking can be seen directly.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a read-only visual overlay driven by the latest landmarks — no change to the tracking pipeline, signals, or CSV.

## Context

Tier-4 enhancement (beyond the written v1 scope, queued deliberately). The `camera` foreground service owns the camera (`specification.md` §Architecture) and the Activity may not own it, so the overlay must render from landmark data surfaced via `StateFlow`, not a second camera binding. This is the tool that makes the deferred daylight gaze retest immediate: look at the screen and watch whether the iris markers follow the eyes.

## Required changes

1. Surface the latest frame's normalised landmark points needed for the overlay (at minimum the two eye corners and the iris centre per eye; the full set if cheap) from the service to the UI via `StateFlow`, throttled/downsampled for drawing and without blocking the analysis loop.
2. Add a Compose `Canvas` overlay that maps normalised coordinates to view space using the **same mirror/orientation convention as the signal adapter** (so left/right and up/down read correctly), highlighting the iris centres so eye movement is visible at a glance.
3. Make the overlay toggleable and clearly read-only; it must not alter tracking, recording, or performance materially (respect the active profile).

## Do not implement

Do not implement:
- a second camera/preview binding in the Activity, or drawing the raw camera video (landmarks-on-canvas is enough for v1);
- changes to signals, detectors, or the CSV;
- gating any recording behind the overlay.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device in good light, enabling the overlay shows dots that track the face, and the iris markers visibly move when the eyes move — making sign/mirroring directly observable.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `015`; still to run are `016`–`018`, plus any added later) assume the codebase as described when they were written. Note: this overlay is the tool most likely to reveal the on-device truth about the eye-movement sign/mirroring convention (`SignConvention`, `FaceMeshIndices`) — if it does, that is exactly the kind of new information this check is for.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`015_face_iris_overlay.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
