# Task: Eye-look-blendshape gaze source

## Goal

Add an eye-look-blendshape-derived gaze as a selectable signal source, recorded via
`tracking_mode`, alongside the existing iris source.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is the adapter computation + a source selector + UI/persistence; it reuses the existing eye-local columns. No new CSV columns, no calibration.

## Context

The night-time on-device recordings showed a near-zero iris-centre signal (iris-from-landmarks
is fragile in low light). MediaPipe's Face Landmarker also emits eye-look blendshapes
(`eyeLookInLeft/OutLeft/UpLeft/DownLeft` and the `*Right` equivalents) that encode gaze
robustly, including in poor light. `FaceSignalAdapter` already reads blendshapes for blink, and
the spec (prompt 029) now defines `tracking_mode ∈ {iris, blendshape}`.

## Required changes

1. In `FaceSignalAdapter`, add a blendshape-derived eye-local: per eye, horizontal =
   participant-right-positive combination of look-in/look-out, vertical = look-up minus
   look-down; populate the same `EyeFeature` x/y as the iris path. Keep the iris path intact.
2. Add a signal-source selector (`SessionConfig`: `iris` / `blendshape`, default `iris`) and a
   UI toggle; record the active source in the CSV `tracking_mode` column and the meta sidecar.
3. Persist the selection (`AppSettings`) and show the active source in the readout.

## Do not implement

Do not implement:
- new CSV columns or calibration / point-of-gaze;
- removing or changing the iris path;
- detector-threshold changes.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- toggling the source changes the eye-local readout to the blendshape-derived gaze, `tracking_mode` records the source, and the choice persists across launches.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `030`; still to run: `031`–`033`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from the plan (e.g. the blendshape signal range/shape differs from what `031` assumes for fitting). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`030_blendshape_gaze_source.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
