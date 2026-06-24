# Task: Optional, consent-gated raw-video capture

## Goal

Offer local raw-video recording as an explicit opt-in that runs alongside tracking, off by
default.

## Scope

The optional video path only. No changes to tracking, signals, or the CSV schema.

## Context

Builds on the camera service and session model. The privacy rule is binding
(`specification.md` §Scope/§Domain rules + report §17): **raw video disabled by default,
recorded only after explicit user activation, stored locally, never auto-enabled**.

## Required changes

1. Add an off-by-default setting with an explicit consent step that, when enabled, binds a
   CameraX `VideoCapture` use case alongside the existing `ImageAnalysis` so a session can
   record an MP4 to app-local storage without interrupting tracking.
2. Record the raw-video filename/path and the consent flag in the session metadata, and key
   the video timeline to the canonical clock so it aligns with the CSV.
3. Make the recording state unmistakable in the UI and notification while video is being
   saved.

## Do not implement

Do not implement: uploading/streaming video anywhere, enabling it by default, hiding the
recording state, or changing the tracking pipeline.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: with the option off (default) no video is written; after explicit opt-in, a
  session also produces a local MP4 while tracking continues, with the recording state
  shown; the video and CSV share the canonical timeline.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`010_optional_raw_video.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
