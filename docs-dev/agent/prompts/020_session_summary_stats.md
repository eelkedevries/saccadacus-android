# Task: Per-session summary statistics

## Goal

Compute per-session summary statistics at stop, write them to a summary sidecar, and show them in the UI.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is an aggregate sidecar plus a UI summary — no change to the main CSV schema or to detection.

## Context

Events (saccades, blinks, and fixations from prompt 019) are detected at stop in `finalizeSessionCsv`; there is no aggregate summary. A summary lets the user judge a session at a glance. Sidecars already exist (`meta_<stamp>.csv`, `sensors_<stamp>.csv`) keyed by the session stamp (prompt 018).

## Required changes

1. At stop, compute summary stats from the recorded samples/events: session duration; counts and per-minute rates of saccades, blinks, and fixations; mean/median saccade amplitude and duration; mean fixation duration; mean reliability; total tracking-loss time.
2. Write them to a `summary_<stamp>.csv` sidecar (`key,value`, `Locale.ROOT`), linked by the same session stamp as the CSV.
3. Show a concise summary in the UI after a session stops (and surface it in the sessions screen where practical).

## Do not implement

Do not implement:
- changes to detection or the main CSV schema;
- charts/graphs or external analytics;
- cross-session aggregation.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- after a session, a `summary_<stamp>.csv` exists with the stats and the UI shows a readable summary.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `020`; still to run: `021`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`020_session_summary_stats.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
