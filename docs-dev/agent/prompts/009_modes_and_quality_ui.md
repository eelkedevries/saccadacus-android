# Task: Tracking-mode selection and live quality-feedback UI

## Goal

Let the user pick a tracking mode before starting, and replace the probe readout with a
proper live tracking-quality panel.

## Scope

Mode selection + the quality UI only. No new tracking maths, persistence, or raw video.

## Context

Builds on `005`–`008`. The modes (reading / experimental task / natural use) and the
selectable eye outputs come from the proposal/overview; quality signals (per-eye and
per-signal reliability, blink, tracking-loss) already exist from `005`/`007`.

## Required changes

1. Add a pre-session **mode selector** (reading / experimental task / natural smartphone
   use) and an eye-output selector (left / right / binocular / both), recorded into the
   session metadata. Per-mode configuration may differ only where already supported (e.g.
   default profile from `004`); do not invent new capabilities.
2. Replace the raw probe text with a **quality panel**: per-eye and per-signal reliability,
   current blink state, a clear tracking-loss / "face lost" indicator, and the active mode.
3. Keep the notification controls and the Start-while-visible rule intact.

## Do not implement

Do not implement: gaze mapping / follow-the-dots, absolute point-of-gaze, raw video, or
any new signal — surface only what already exists.

## Acceptance criteria

- `./gradlew assembleDebug` builds in CI.
- On device: a mode + eye-output can be chosen before Start and appear in the exported
  session metadata; the quality panel updates live and clearly shows when the face is lost.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Commit and push

If the scope was followed and the build passes, commit on `main` using this file's exact
filename (`009_modes_and_quality_ui.md`) as the commit message, then push.

## Final report

End with the required final report specified in `AGENTS.md`.
