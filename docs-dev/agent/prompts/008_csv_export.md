# Task: Combined-CSV export and share

## Goal

Write a recorded session to the canonical combined CSV and let the user export/share it,
written incrementally so a long or interrupted session is never lost.

## Scope

Persistence + export only, against the schema already fixed in the spec. No new signals,
modes, or raw video.

## Context

Builds on `007`. The exact CSV is fixed in `specification.md` §Data schemas (single
combined file; `row_type ∈ time_series | event | task`; the listed columns; canonical +
camera + wall-clock timestamps; `Locale.ROOT` formatting; `pupil_*` left empty). Departures
from the browser export are noted there and in `saccadacus_phase0_audit.md`.

## Required changes

1. Write the session to the combined CSV **incrementally** (append rows during the
   session), with locale-independent number formatting and the exact column set/order from
   the spec; mark tracking-loss intervals.
2. Finalise atomically (write to a temp file, flush/fsync, rename) so a crash leaves a
   valid partial file.
3. Add an **Export/Share** action that surfaces the finished CSV via the Storage Access
   Framework / system share sheet.

## Do not implement

Do not implement: Room/SQLite/Parquet/Protobuf (CSV only for v1), tracking modes, gaze
mapping, or raw video.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: after a session, an Export action produces a single CSV that opens cleanly in
  a spreadsheet / R / Python, with the spec's columns and `.` decimals regardless of
  locale; a force-killed session still leaves a readable partial file.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI); open an exported CSV.

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`008_csv_export.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
