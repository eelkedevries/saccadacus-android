# Task: Session naming + notes

## Goal

Let the user name a session and add a free-text note, saved into the session metadata.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is name/note capture and persistence into session metadata, surfaced where sessions are shown — no change to the CSV time-series schema.

## Context

Tier-4 enhancement (beyond the written v1 scope, queued deliberately). A session metadata sidecar already exists (prompt 008). Naming and notes aid experiment tracking and complement interaction markers (prompt 012) and the sessions screen (prompt 017).

## Required changes

1. Let the user set an optional name and note before starting and/or after stopping a session, and persist them in the session metadata. If the name is reflected in the file/sidecar naming, do so **without breaking the `session_*.csv` discovery** that export and the sessions list rely on.
2. Show the name and note in the sessions screen (prompt 017) and in the main readout; when the name is blank, fall back to a sensible auto-name (the timestamp).
3. Keep names filesystem-safe and the UI text British English.

## Do not implement

Do not implement:
- rich text or attachments;
- per-row labels (that is interaction markers, prompt 012);
- renaming the contents of arbitrary past CSV files, or cloud storage.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, a named session shows its name/note in the sessions list and metadata, blank names fall back to the timestamp, and export/discovery still works.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence. This is currently the last queued prompt (`018`); there are no later prompts unless earlier execution added some. If a `019+` exists when this runs, apply the same check to it.

While executing this prompt, watch for anything that departs from the plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`018_session_naming_notes.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
