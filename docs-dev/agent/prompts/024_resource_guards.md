# Task: Resource guards (storage + thermal)

## Goal

Warn — and on a critical condition, safely stop — when the device is low on storage or overheating during a long session.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is advisory monitoring plus a safe auto-stop — no change to detection or the signal path.

## Context

The report (§15) flags thermal and storage as long-session risks; raw video and long recordings can exhaust space or overheat the device. Tracking is always user-visible, so any automatic action must surface a clear message.

## Required changes

1. Periodically check free storage (`StatFs` on the external files dir); below a warning threshold show a warning in the UI/notification, and below a critical threshold stop the session safely (finalising the CSV).
2. Monitor thermal status (`PowerManager` thermal status / listener, API 29+); on a severe/critical status show a warning, and on critical stop the session safely.
3. Surface both states advisorily; never act silently — every automatic stop must be accompanied by a visible reason.

## Do not implement

Do not implement:
- deleting user data to free space;
- mid-session profile downgrades or aggressive throttling;
- changing detection or recording format.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- a simulated low-storage or severe-thermal condition shows a warning, and the critical condition triggers a clean auto-stop with a visible reason.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `024`; still to run: `025`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`024_resource_guards.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
