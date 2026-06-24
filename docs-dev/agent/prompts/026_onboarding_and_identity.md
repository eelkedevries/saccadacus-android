# Task: Onboarding, permission rationale, and app identity

## Goal

Add a first-run explainer with permission rationale, and tidy the app's name/theme identity.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a one-time onboarding screen, a pre-request rationale, and light identity polish — no new tracking behaviour.

## Context

The app currently jumps straight to requesting the camera. A brief explainer (what is recorded, that it is user-initiated and visible, why camera/notification are needed) and basic identity make it presentable. A persisted first-run flag can reuse the DataStore from prompt 025.

## Required changes

1. A first-run screen (gated by a persisted flag) explaining what the app records, that recording is user-initiated and always visible (notification + camera indicator), and why camera and notification permissions are needed; then it proceeds to the control screen.
2. Show a short rationale before the permission request when the permissions are not already granted.
3. Tidy identity: the app-name string, theme colours, and user-facing strings in British English. Keep the existing launcher icon (no new binary icon assets).

## Do not implement

Do not implement:
- analytics, accounts, or login;
- elaborate theming or new icon artwork;
- changing the recording pipeline.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- first launch shows the explainer once; subsequent launches skip it; the rationale appears before a permission request; identity strings/colours are updated.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `026`; still to run: `027`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected (e.g. `027` docs should match the onboarding copy); if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`026_onboarding_and_identity.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
