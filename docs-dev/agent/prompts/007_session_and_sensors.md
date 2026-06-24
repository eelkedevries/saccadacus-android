# Task: Session model and device-orientation / motion sensors

## Goal

Record a session — per-frame signals, events, sensor samples, and metadata — in memory
between Start and Stop, and capture device orientation and motion alongside the camera
signals.

## Scope

The in-memory session model + sensor capture + a live quality summary. Persistence/export
is the next prompt. No new tracking maths.

## Context

Builds on `005`/`006`. The canonical clock and the recorded fields are fixed in
`specification.md` §Data schemas (elapsed-realtime-nanos canonical, wall-clock anchor,
camera sensor timestamp + source). Sensors and coordinate frames are described in the
research report §14.

## Required changes

1. Define a `Session` holding: start/stop wall-clock anchors, the canonical-clock series,
   buffered samples and events (from 006), and camera/processing metadata.
2. Capture `TYPE_ROTATION_VECTOR` plus raw gyroscope and accelerometer via `SensorManager`,
   each with its own timestamp domain, recorded into the session (kept separate, not
   pre-fused) along with display rotation.
3. Record a tracking-loss interval whenever the face is lost or the camera is evicted.
4. Show a live session summary (duration, frames, events, current reliability, sensors OK).

## Do not implement

Do not implement: CSV export (prompt 008), tracking modes, gaze mapping, or raw video.
Do not hold raw camera images in the session.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: a session records start→stop; the summary shows growing frame/event counts
  and live sensor values; losing the face creates a tracking-loss interval.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`007_session_and_sensors.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
