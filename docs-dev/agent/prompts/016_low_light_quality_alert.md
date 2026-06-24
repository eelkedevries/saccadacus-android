# Task: Low-light / face-lost quality alert

## Goal

Warn the user on-screen and in the notification when lighting is too low or the face is lost, so unreliable recordings are flagged while they happen.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a lightweight quality signal plus alert surfacing — no changes to detector thresholds or recorded data.

## Context

Tier-4 enhancement (beyond the written v1 scope, queued deliberately). It fits `specification.md` §Scope "live tracking-quality feedback". A low-light session during development showed that MediaPipe iris tracking degrades badly in poor light (the iris landmark collapses toward the eye centre); flagging it live prevents wasted recordings.

## Required changes

1. Compute a simple per-frame lighting/quality indicator (for example mean luma of the analysis frame, and/or the existing face-lost / low-reliability signal) and a smoothed state of `good` / `low_light` / `face_lost`, surfaced via `StateFlow`.
2. Show the state prominently in the UI and reflect a concise warning in the notification (for example "Low light — tracking may be unreliable") while it persists.
3. Keep it strictly advisory — never auto-stop or alter recording; recorded data is unaffected.

## Do not implement

Do not implement:
- auto-exposure or camera-control changes;
- changing detector thresholds or the saccade/blink logic;
- blocking or pausing recording automatically;
- new CSV time-series columns (metadata only, if trivial and non-breaking).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, dim lighting or covering the camera flips the indicator to a visible warning in-app and in the notification, and it clears when lighting is restored.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `016`; still to run are `017`–`018`, plus any added later) assume the codebase as described when they were written.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`016_low_light_quality_alert.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
