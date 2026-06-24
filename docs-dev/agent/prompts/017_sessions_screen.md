# Task: Sessions screen

## Goal

Add an in-app list of past recordings with per-session export-to-Downloads, share, and delete.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is a read/manage screen over the existing session files — no change to recording or the CSV schema.

## Context

Tier-4 enhancement (beyond the written v1 scope, queued deliberately). Sessions are `session_*.csv` files in the app's external files dir; the main screen already has "Save CSV to Downloads" (latest only, via MediaStore) and Share. This generalises management to all sessions.

## Required changes

1. Add a sessions list (Compose) enumerating saved session CSVs with a human-readable date/time, a size or row/duration summary, and any associated sidecar/video if present.
2. Per session, offer export-to-Downloads (reuse the existing MediaStore save), share, and delete (with confirmation); reflect changes in the list immediately.
3. Navigate to and from the main screen without disrupting an active session, and never offer destructive actions on the in-progress session's file.

## Do not implement

Do not implement:
- cloud sync or upload;
- renaming files in place (naming is prompt 018) or editing CSV contents;
- background/automatic deletion.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- on device, completed sessions appear in the list; export drops the chosen file into Downloads; delete removes it after confirmation; and the active session is protected.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `017`; still to run is `018`, plus any added later) assume the codebase as described when they were written. Note: prompt 018 (session naming + notes) will want this list to display names — keep that in view when deciding whether 018 needs adjusting.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`017_sessions_screen.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
