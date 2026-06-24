# Task: Interaction markers (task rows)

## Goal

Let the user drop a timestamped, optionally-labelled interaction marker during a session, written as a `task` row in the combined CSV.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is marker capture, the task-row writer, and a trigger control — using existing columns, with no schema change.

## Context

`specification.md` §Scope counts **interaction markers** as in-scope (they are the boundary between v1 and the out-of-scope cross-app synchronisation); the report (§12) names the interaction-marker plus the wall-clock anchor as the minimal v1 cross-app-sync primitive. The combined CSV already defines `row_type = task` and the `task_marker`/`annotation` columns (`CsvSessionWriter.HEADER`) but nothing emits them.

## Required changes

1. Add `CsvSessionWriter.appendTask(...)` that writes a row with `row_type = task`, the canonical `elapsed_realtime_nanos` timestamp, an optional label in `task_marker`, and optional free text in `annotation` — consistent with the existing row writers and `Locale.ROOT` formatting.
2. Add a service action (e.g. `ACTION_MARK`) and a "Mark" control in both the UI and the notification that, during an active session, records a marker at the current canonical timestamp; markers are ignored when no session is active.
3. Surface a simple confirmation (e.g. count of markers this session) in the UI `StateFlow`.

## Do not implement

Do not implement:
- cross-device networking / WebSocket synchronisation;
- editing or deleting markers, or capturing markers while idle;
- any change to the CSV schema or to other row types.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, tapping Mark during a session adds a `task` row (visible in the exported CSV) with the correct monotonic timestamp and any label, and the marker count shows in the UI.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `012`; still to run are `013`–`018`, plus any added later) assume the codebase as described when they were written.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`012_interaction_markers.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
