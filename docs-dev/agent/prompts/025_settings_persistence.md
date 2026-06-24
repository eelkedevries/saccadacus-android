# Task: Settings persistence (DataStore)

## Goal

Persist user-chosen settings across app launches.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is persistence of the existing config objects via DataStore — no new user-facing settings.

## Context

`ProbeConfig.selected`, `SessionConfig.*` (use-case/eye modes, raw video, name/note, and the prompt-021 filter flag), and `OverlayConfig.enabled` are in-memory `@Volatile` values that reset on process death. The report (§13) calls for configuration in DataStore.

## Required changes

1. Add the Preferences DataStore dependency via the version catalogue, and a small settings repository that persists: selected profile, use-case mode, eye mode, raw-video toggle, overlay toggle, the filter toggle (021), and the session name/note defaults.
2. Load persisted values into the existing in-memory config objects at startup, and save on change.
3. Keep the in-memory config objects as the single source of truth at runtime (DataStore hydrates and mirrors them).

## Do not implement

Do not implement:
- persisting transient runtime state (frame counts, live stats);
- cloud sync or a settings UI screen;
- a database — Preferences DataStore only.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- changing settings and relaunching the app restores them.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `025`; still to run: `026`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected (e.g. `026` will store a first-run flag — it can reuse this DataStore); if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`025_settings_persistence.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
