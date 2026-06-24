# Task: On-device backend benchmark and operating profiles

## Goal

Measure whether the Face Landmarker runs fast enough on real devices, and define
quality / balanced / battery-saving profiles from the result — retiring the on-device
performance risk before more is built on top.

## Scope

Add a benchmark mode and the profile definitions only. This is the risk-first perf stage
(research report §15/§19). Do not build tracking signals or events.

## Context

Builds on `003` (MediaPipe integrated). Performance figures must be **measured, not
assumed** (`specification.md`/report evidence rules). The app builds in CI but only a
human can run the on-device benchmark.

## Required changes

1. Add a benchmark toggle/screen that, during a session, records per-frame: inference
   latency, achieved analysed fps, and dropped/late frames; compute mean / median / p95 /
   p99 latency and mean fps over the run, shown on screen and appended to a `benchmark_*.csv`
   in the app's external files dir.
2. Define three operating profiles — **quality / balanced / battery-saving** — as
   data (target resolution, target fps, inference cadence). Wire a profile selector that
   changes the `ImageAnalysis`/inference cadence accordingly.
3. Document how to run the benchmark and where the CSV lands (short note in
   `docs-dev/planning/` or the screen text).

## Do not implement

Do not implement: eye/iris/head-pose derivation, events, the session model, export schema,
thermal-throttling automation (just record, don't adapt), or raw video.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: running a session in each profile produces on-screen latency percentiles +
  fps and a `benchmark_*.csv`.
- Profile selection visibly changes the analysed frame rate.
- Record the measured numbers per test device in `docs-dev/planning/current_state.md`
  (human step) so later profile defaults are evidence-based.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI), then the on-device benchmark on at
least one device.

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`004_backend_benchmark.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
