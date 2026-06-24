# Task: User docs (usage, CSV dictionary, privacy)

## Goal

Add user-facing documentation: how to use the app, a CSV column dictionary, and a privacy/data-handling note.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is documentation under `docs/` only — no code or behaviour change.

## Context

`docs/` holds user-facing, publishable docs (kept out of the deployed build by `scripts/check-public-build.sh`; never include `docs-dev/` content). The recorded CSV needs a column dictionary, and a camera app that records faces needs a clear privacy note (report §17).

## Required changes

1. `docs/usage.md` — install from Releases → latest, record, pause/mark/stop, export to Downloads, and the sessions screen.
2. `docs/csv_schema.md` — every combined-CSV column plus the sidecars (`meta_`, `sensors_`, `summary_`) explained, including the canonical-clock note (`elapsed_realtime_nanos` + wall-clock anchor) and the `row_type`/`event_type` values (time_series, event = saccade/blink/fixation, task).
3. `docs/privacy.md` — what is and is not recorded, that processing is fully on-device, raw video is opt-in, where data is stored, and how to delete it.

## Do not implement

Do not implement:
- a marketing site or auto-generated docs;
- any code or behaviour change;
- copying `docs-dev/` material into `docs/`.

## Acceptance criteria

The task is complete when:
- the three docs exist under `docs/` and are accurate to the current build;
- `./gradlew assembleDebug` still builds in CI (no code changed).

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `027`; still to run: `028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected; if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`027_user_docs.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
