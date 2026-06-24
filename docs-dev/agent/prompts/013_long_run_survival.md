# Task: Long-run survival — battery exemption + camera-eviction recovery

## Goal

Improve survival of long background sessions by offering a battery-optimisation exemption and by recovering automatically from camera eviction / tracking loss.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is the exemption-request flow plus camera re-acquire / loss handling in the service — no algorithm changes.

## Context

The report (§20) lists OEM/screen-off survival as the **#1 risk**; the listed mitigations are the foreground service (done), an optional battery-optimisation-exemption request, and detecting camera eviction → logging a tracking-loss interval → attempting re-acquire. Tracking-loss intervals are already counted (prompt 007). Initiation must stay user-aware (`specification.md` §Domain rules).

## Required changes

1. Add a user-initiated flow to request ignoring battery optimisations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission + the corresponding intent), surfaced as an optional, clearly-explained button — never required, never silent.
2. Detect camera unbinding/eviction or a prolonged frame gap during an active session, log it as a tracking-loss interval (consistent with existing loss handling), and attempt to re-acquire / re-bind the camera automatically, returning to normal when frames resume.
3. Reflect a degraded "camera lost — retrying" state in the UI and notification while re-acquiring, clearing it when frames return.

## Do not implement

Do not implement:
- OEM-specific or rooted hacks, or disabling the system camera indicator;
- forcing the screen to stay on, or silently restarting without user awareness;
- changes to detectors, signals, or the CSV schema.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, the exemption button opens the correct system dialog, and if the camera is taken by another app and then released, the service logs a loss interval and resumes tracking without a manual restart.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `013`; still to run are `014`–`018`, plus any added later) assume the codebase as described when they were written.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`013_long_run_survival.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
