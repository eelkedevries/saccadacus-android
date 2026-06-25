# Task: Export the full session bundle to Downloads

## Goal

When the user saves a session to Downloads, export **all of that session's files** — the
combined CSV plus every sidecar (meta, summary, sensors, and any benchmark / frame-log / raw
video) — not just the session CSV.

## Scope

Implement only the work described in this prompt. It (a) unifies the remaining per-session
sidecar filenames onto the session stamp so a session is one identifiable set, and (b) makes the
"Save to Downloads" actions copy the whole set. It does not change what data each file contains,
the recording pipeline, or sharing via the share-sheet.

## Context

A session writes a combined `session_<stamp>.csv` plus sidecars, but only `meta_<stamp>.csv` and
`summary_<stamp>.csv` currently use the session stamp — `sensors_`, `frame_log_`, `benchmark_`
and the raw `video_` files are each named with their own `System.currentTimeMillis()`, so they
cannot be matched to a session. And "Save CSV to Downloads" (plus the per-session **Save** button)
copies only the session CSV. As a result the companion files — including `meta`, which holds the
**calibration validation error** — are stranded in the app's private storage with no way out.

## Required changes

1. In the camera service, name the remaining per-session output files with the **session stamp**
   (`sensors_<stamp>.csv`, `frame_log_<stamp>.csv`, `benchmark_<stamp>.csv`, `video_<stamp>.mp4`),
   matching `meta_`/`summary_`. The stamp is already set at session start, so reuse it; do not
   change file contents or the meta `raw_video_path` semantics (it stays an absolute path).
2. In the save-to-Downloads path, gather the **bundle** for a session — the session CSV plus
   every file in the same directory whose name contains that session's stamp — and copy each into
   public Downloads, choosing the MIME type by file extension (CSV, MP4, else octet-stream).
   Show a single summary toast (how many files saved). Route both the main "Save … to Downloads"
   button and the per-session **Save** button through this.
3. Update the button label and the user docs (`docs/usage.md`, `docs/csv_schema.md`) to say the
   save action now exports the whole session bundle, not just the CSV.

## Do not implement

Do not:
- change recording, the CSV schema, or any file's contents;
- add deletion/sorting of Downloads or de-duplication (MediaStore already disambiguates names);
- change the share-sheet (`ACTION_SEND`) path or add cloud upload;
- bundle the standalone benchmark when it is not part of a session (only files sharing the
  session stamp are exported).

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- saving a session to Downloads copies the session CSV **and** its `meta`/`summary`/`sensors`
  (and benchmark / frame-log / video when present), all sharing the session stamp;
- the button label and docs reflect the bundle export.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`036`); there are no later
prompts unless earlier execution added some. If a `037+` exists when this runs, apply the same
check to it. While executing this prompt, watch for anything that departs from the plan; after the
required changes are done and **before committing**, if a later not-yet-run prompt exists and is
affected, edit it in this same commit and note it under "Scope deviations"; otherwise say nothing
downstream is affected. Do not edit already-run prompts, do not renumber, and do not expand this
prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this
file's exact filename (`036_export_session_bundle.md`) as the commit message, then push. Do not
commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
