# Task: Expand unit tests

## Goal

Add pure-JVM unit tests for logic added since prompt 006 that has no Android dependency.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is JVM unit tests (run by `testDebugUnitTest`) plus, at most, pure refactors needed to make logic testable — no behaviour change.

## Context

Prompt 006 added saccade/blink parity tests (`app/src/test/...`). The eye-local projection (`EyeLocal.kt`, pure Kotlin) and the fixation detector (prompt 019, pure Kotlin) are untested. CI already runs `testDebugUnitTest`.

## Required changes

1. Add tests for `projectEyeLocal`: a known corner/iris geometry maps to the expected `xLocal`/`yLocal`, and degenerate (zero-width) corners return `(0, 0)`.
2. Add tests for the fixation detector: a synthetic stable-then-saccade-then-stable sequence yields the expected fixation interval(s) and ignores too-short runs.
3. Keep all tests pure-JVM (no Android framework types, no instrumentation); do not weaken production code to pass tests beyond pure, behaviour-preserving refactors.

## Do not implement

Do not implement:
- instrumented/UI tests or a device test harness;
- tests that require Android types (e.g. `CsvSessionWriter`'s `android.util.Log`/`File`);
- changes to detector behaviour or thresholds.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds and `./gradlew testDebugUnitTest` passes in CI, including the new tests.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI; CI also runs `testDebugUnitTest`).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`028`); there are no later prompts unless earlier execution added some. If a `029+` exists when this runs, apply the same check to it. While executing this prompt, watch for anything that departs from the plan; after the required changes are done and **before committing**, if a later not-yet-run prompt exists and is affected, edit it in this same commit and note it under "Scope deviations"; otherwise say nothing downstream is affected. Do not edit already-run prompts, do not renumber, and do not expand this prompt's own scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`028_unit_tests_expansion.md`) as the commit message, then push. Do not commit partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
