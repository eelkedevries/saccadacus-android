# Task: Top app bar + sub-screen polish

## Goal

Finish the GUI redesign: add a Material-3 top app bar with per-screen titles + a single Back action,
and polish the Sessions / Onboarding / Calibration sub-screens so they match the new card-based main
screen and are easy to navigate.

## Scope

Presentation + navigation only. Centralise screen routing in an `AppRoot` that hosts one `Scaffold`
with a `TopAppBar`; render the full-screen Calibration and Onboarding outside it (so calibration keeps
the full screen). Polish the sub-screens with the 052 UI kit. No change to tracking, recording, CSV,
calibration maths, or model logic.

## Context

052 restructured the main screen into cards but left routing inside `ControlScreen` (boolean +
early-return) and the sub-screens as plain scrolling columns with their own Back buttons. This prompt
lifts routing to a parent so the app bar can show the right title + Back, and gives the Sessions list a
card per session.

## Required changes

1. `MainActivity` hosts `AppRoot()`: onboarding (full-screen) → then a `Scaffold` + `TopAppBar` for the
   **Control** ("Saccadacus") and **Sessions** ("Sessions" + Back) screens, with **Calibration**
   rendered full-screen (no app bar — it needs the whole screen) reached from the control screen.
2. `ControlScreen` takes `onOpenSessions` / `onOpenCalibration` callbacks instead of its own routing,
   and drops its in-column title (the app bar shows it).
3. `SessionsScreen` (no own Back) renders each session as a titled card (name, note, size/date, and
   Save / Share / Delete), with an empty state.
4. `OnboardingScreen` and `CalibrationScreen` stay full-screen: handle their own system-bar insets and
   get a light visual polish (Onboarding uses the card kit; Calibration keeps its full-screen field).

## Do not implement

Do not:
- change tracking / recording / CSV / calibration maths / model behaviour;
- add an icon dependency;
- alter the 052 card content beyond removing the now-duplicate title and wiring the nav callbacks.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- the app shows a top app bar with a Back action on Sessions, Calibration runs full-screen, and the
  Sessions list is a card per session; all prior navigation still works.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Builds on `052` (UI kit + card layout). This is currently the last queued
prompt (`053`); if a `054+` exists when this runs, apply the same check to it. Do not edit already-run
prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`053_ui_app_bar_and_subscreens.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
