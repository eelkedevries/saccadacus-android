# Task: Pin & verify SDK / AGP / Compose versions

## Goal

Confirm the build's SDK, AGP, Kotlin/Compose-compiler, and Compose-BOM versions are stable (non-preview) releases, and pin them.

## Scope

Implement only the work described in this prompt. Do not implement adjacent systems or future prompts. This is version verification plus value changes in `gradle/libs.versions.toml` and the build scripts only — no feature code.

## Context

`specification.md` §Locked decisions and the report (§21) both flag: verify that `compileSdk 37`, `AGP 9.2.0`, and the Compose BOM are **stable GA releases, not previews**, "when the build is next touched." Current state: `app/build.gradle.kts` sets `compileSdk = 37`, `targetSdk = 36`; plugin/library versions live in the version catalogue.

## Required changes

1. Verify whether the pinned AGP, `compileSdk`/`targetSdk`, Kotlin/Compose-compiler, and the Compose BOM are stable GA releases (not alpha/beta/rc/preview). Briefly record what was checked and the finding.
2. If any is a preview/unstable, pin it to the latest stable GA equivalent in the version catalogue / build script. If all are already stable, make no version change.
3. Keep CI green; do not bump versions speculatively beyond what stability requires, and do not change `minSdk`.

## Do not implement

Do not implement:
- unrelated dependency upgrades or new build features;
- `minSdk` changes or new Gradle plugins;
- speculative "latest everything" bumps.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` builds in CI;
- every compile-affecting version is a confirmed GA release (or pinned to one), with a one-line note of what was checked/changed, and CI stays green.

## Checks

Run `./gradlew assembleDebug --no-daemon` (via CI).

## Cross-prompt impact check

This project runs prompts in sequence; later prompts in `docs-dev/agent/prompts/` (this is `014`; still to run are `015`–`018`, plus any added later) assume the codebase as described when they were written. A version pin here can change available APIs that a later prompt relies on, so this check matters.

While executing this prompt, watch for anything that departs from that plan — a change you had to make differently, an assumption that turned out wrong, a renamed symbol or file, or new information about the stack or device behaviour. After the required changes are done and **before committing**:

1. Decide whether any of it affects a later, not-yet-run prompt (its Goal, Scope, Required changes, or Acceptance criteria).
2. For each affected later prompt, examine whether it is still correct and achievable as written.
3. If it needs adjusting, edit that later prompt file in this same commit so the queue stays accurate, and record the change (which prompt, what, why) under "Scope deviations" in the final report.
4. If nothing downstream is affected, say so explicitly in the final report.

This downstream-prompt maintenance is explicitly authorised by this prompt and is not scope creep. Do not edit prompts that have already been run, do not renumber files, and do not expand this prompt's own implementation scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's exact filename (`014_pin_sdk_agp_versions.md`) as the commit message, then push. Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
