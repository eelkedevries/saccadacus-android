# Task: Orphaned-session recovery

## Goal

Recover a session left as a `.tmp` file when the service was killed mid-session, so its data is not lost.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is launch-time recovery of leftover temp files — no schema change.

## Context

`CsvSessionWriter` writes `session_<stamp>.csv.tmp` during a session and atomically renames it to `.csv` at stop (prompt 008). If the service is killed, a readable `.tmp` remains, which `listSessionCsvs` (filters `.csv`) never surfaces, so the data is invisible.

## Required changes

1. On app launch, when no session is active, find leftover `session_*.csv.tmp` files and finalise them by renaming each to its `.csv` form (best-effort recovery of the time-series already written).
2. Never touch the `.tmp` of a session that is currently in progress.
3. Recovered files then appear as normal entries in the sessions screen (prompt 017).

## Do not implement

Do not implement:
- reconstructing the saccade/blink/fixation event rows (those are computed at stop; recovery keeps the time-series only);
- deleting partial data;
- background/automatic recovery while a session is running.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- a leftover `session_*.csv.tmp` is renamed to `.csv` on the next launch and appears in the sessions list, while an in-progress session's `.tmp` is left untouched.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `022`; still to run: `023`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`022_orphaned_session_recovery.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
