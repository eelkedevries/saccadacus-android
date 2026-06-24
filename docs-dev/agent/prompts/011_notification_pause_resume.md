# Task: Notification pause/resume controls

## Goal

Add pause and resume controls to the camera foreground service and its notification, alongside the existing stop.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is the service run-state and notification only — no tracking, algorithm, or CSV-schema changes.

## Context

`specification.md` §Scope and the report's staged plan (§19, stage 2) both specify notification controls as **pause/resume/stop**; only stop exists today (`CameraTrackingService` has `ACTION_START`/`ACTION_STOP`). Tracking is user-initiated while visible (`specification.md` §Domain rules), and the foreground-service notification + system camera indicator must always remain visible — so a pause must not tear down the service.

## Required changes

1. Add `ACTION_PAUSE` and `ACTION_RESUME` to the service. Pausing stops feeding frames to the model and stops recording samples (and pauses raw video if it is recording — prompt 010) while keeping the foreground service, the camera binding, and the notification alive; resuming continues the **same** session.
2. Reflect the paused state in the notification (swap the action button between Pause and Resume and update the text) and in the UI via `StateFlow`, so the Activity shows paused vs running.
3. Preserve session and timestamp continuity across pause/resume (a pause is a gap in samples, not a new session); if a pause is recorded at all, record it consistently with the existing tracking-loss handling and **without** changing the CSV schema.

## Do not implement

Do not implement:
- starting a new session on resume, or tearing down the foreground service on pause;
- automatic/silent pausing, or hiding the notification or camera indicator;
- any change to the analysis pipeline, detectors, or CSV columns.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, Pause halts the rising frame/sample counters while the notification and camera indicator persist, Resume continues the same session, and Stop still ends it.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `011`; still to run are `012`–`018`, plus any added later) assume the codebase as described when they were written.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`011_notification_pause_resume.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
