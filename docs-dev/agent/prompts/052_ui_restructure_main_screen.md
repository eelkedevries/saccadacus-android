# Task: Restructure the main screen into clear sections (UI kit)

## Goal

Make the main control screen easy to scan and use: group the controls and read-outs into clearly
titled Material-3 cards, replace the discoverability-hostile controls (selection-by-disabling, tap-to-
cycle toggles, ON/OFF text buttons) with selectable chips and switches, make Start/Stop the prominent
primary action, and tuck the detailed diagnostics into a collapsible section.

## Scope

Presentation only. Add a small reusable UI kit and rewrite `ControlScreen`'s main layout to use it. No
change to tracking, recording, CSV, calibration maths, or model logic — every existing action and
read-out stays, just reorganised and relabelled. The top app bar + sub-screen (Sessions / Calibration
/ Onboarding) polish are a separate follow-up (053).

## Context

The current `ControlScreen` is one long flat `Column`: rows of buttons where the *selected* one is
disabled, tap-to-cycle toggles that hide their other states ("Gaze source: iris (tap to switch)"),
ON/OFF text buttons that should be switches, the primary Start/Stop buried mid-scroll, and a wall of
unlabelled status text. The theme is already Material 3 (teal/slate/amber + dynamic colour). No icon
library is on the classpath, so the kit is text/chip/switch based.

## Required changes

1. Add `ui/UiKit.kt` (package `…ui`): reusable `SectionCard(title, description?, content)`,
   `LabeledSwitch(label, checked, onCheckedChange, description?, enabled)`, a generic single-select
   `ChipSelector(label, options, selected, onSelect, …)` (selected chip is highlighted, not disabled),
   and `StatRow(label, value)`. Stable M3 components only (Card / Switch / FilterChip / FlowRow).
2. Rewrite `ControlScreen`'s main `Column` into titled cards:
   - **Recording** (hero): live state line + elapsed/rate/lighting read-outs + a full-width
     **Start/Stop** primary button; Pause/Resume/Mark when running (with a one-line note on "Mark").
   - **Recording setup** (controls disabled while running): session name/note, Quality profile / Use
     case / Eyes / Gaze source as chip selectors, the CNN **Model** selector (chips) only when the
     source is CNN, Smoothing / Save raw video / Camera overlay switches, and the "Allow background"
     action — each with a short helper line.
   - **Gaze calibration**: status (calibrated + check error) + the Calibrate button.
   - **Saved data**: Save-to-Downloads / Share / All-sessions + the last-session summary.
   - **Details & diagnostics** (collapsible, default hidden): the detailed read-outs, the camera
     overlay canvas, and the "Compare gaze models" reference.

## Do not implement

Do not:
- change tracking/recording/CSV/calibration/model behaviour or any non-UI logic;
- add the top app bar or refactor screen routing (053);
- restyle the Sessions / Calibration / Onboarding screens (053);
- add an icon dependency.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- the main screen renders as titled cards with chip selectors + switches, a prominent Start/Stop, and a
  collapsible Details section; every prior control/read-out is still present and wired to the same
  state.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. Follow-up `053` adds the top app bar + routing and polishes the sub-screens;
it builds on the UI kit here. Do not edit already-run prompts, do not renumber, do not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`052_ui_restructure_main_screen.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
