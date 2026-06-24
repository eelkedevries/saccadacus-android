# Task: On-screen + notification frame counter for the feasibility probe

## Goal

Make the prompt-002 survival test checkable at a glance — show a live frame count,
time since last frame, and an alive/blocked verdict in the app and the notification —
so no `adb`/CSV retrieval is needed.

## Scope

Small extension of `002_camera_fgs_poc.md`. Surface the existing frame counter; do not
add tracking, ML, or any new capability. The CSV logging stays as-is.

## Required changes

1. Add a process-wide stats holder (`TrackingStats`) exposing a `StateFlow` snapshot:
   active flag, frame count, session start time, and last-frame time (all
   `elapsedRealtimeNanos`).
2. The service updates it: on start, per analysed frame, and on stop. The service also
   refreshes the persistent notification's text with the running frame count, throttled
   to about once every 2 seconds.
3. The activity observes the snapshot and shows: frames logged, running duration, seconds
   since the last frame, approximate fps, and a plain-language verdict (frames arriving
   vs no recent frame). A ~0.5 s UI tick keeps "since last frame" climbing when frames
   stop, so a failure is visible without reopening anything.

## Do not implement

Do not implement: any tracking/ML, signal/event logic, pause/resume, raw video, or
changes to the CSV schema.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- With the app open, the on-screen frame count visibly increases while tracking; the
  notification shows a frame count that updates periodically.
- After backgrounding and returning, the screen shows whether frames kept arriving
  (count went up; "since last frame" small) — no `adb` needed.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`002b_onscreen_frame_counter.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
