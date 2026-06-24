# Task: Optional eye-local signal filtering

## Goal

Offer an optional smoothing filter on the eye-local signal before event detection, off by default.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is an opt-in filter stage plus a toggle — default behaviour and the prompt-006 parity tests must be unchanged.

## Context

The raw iris-centre eye-local signal is noisy; a light filter can improve event detection. It must be **opt-in** so the default signal path (and the existing parity tests) are byte-for-byte unchanged.

## Required changes

1. Add a configurable filter (one-euro or EMA) applied to each eye's eye-local x/y in the path feeding `SessionRecorder` and event detection, behind a `SessionConfig` flag that defaults to **off**.
2. Add a UI toggle to enable/disable it, and record the filter state (and parameters) in the session metadata.
3. When the flag is off, the signal path must reproduce the current values exactly (no regression).

## Do not implement

Do not implement:
- filtering head pose or blink state;
- changing detector thresholds;
- enabling the filter by default.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- enabling the filter visibly smooths the live eye-local readout, and disabling it reproduces the current behaviour; the meta sidecar records the filter state.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence; later prompts in `docs-dev/agent/prompts/` (this is `021`; still to run: `022`–`028`, plus any added later) assume the codebase as described. While executing this prompt, watch for anything that departs from that plan (a changed assumption, a renamed symbol, new device/stack information). After the required changes are done and **before committing**: decide whether any later, not-yet-run prompt is affected (e.g. `025` should persist the new toggle, `026`/`027` mention it); if so, edit that prompt file in this same commit and record it under "Scope deviations" in the final report; if nothing downstream is affected, say so. This downstream maintenance is authorised by this prompt; do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`021_eye_local_filtering.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
