# Task: Initial project scaffold

## Goal

Confirm the pre-generated **android-kotlin + jetpack-compose** scaffold builds a debug APK, and add
nothing else.

## Scope

Implement only the work described in this prompt. Do not implement adjacent
systems or future prompts.

## Context

This is the first prompt in a new project initialised from the `eek-a-dev`
template. Reference relevant notes in `docs-dev/planning/` if they exist.

## Required changes

1. The Android scaffold (committed Gradle wrapper, `app` module, Compose Material 3 Empty Activity) is already present from the template — do **not** re-create it. Confirm it is intact: `gradle/wrapper/gradle-wrapper.jar` committed, `gradlew` executable, and the `app` module and manifest present.
2. Confirm the debug APK builds — push and let the **Build APK** workflow run, or run `./gradlew assembleDebug --no-daemon` on a machine with the Android SDK.
3. Confirm the README's build-and-sideload note matches the project. Add no features, screens, or release signing.


## Do not implement

Do not implement:
- application features, screens, or content;
- GitHub Pages deployment or CI changes beyond what is already templated;
- tests, state management, or architecture beyond the default scaffold.

## Acceptance criteria

The task is complete when:
- the committed Gradle wrapper and `app` module are intact;
- `./gradlew assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk` (in CI, or locally with the SDK);
- no feature code beyond the default scaffold has been added.

## Checks

Run the relevant checks for this prompt (install dependencies and start the
project).

## Commit and push

If and only if the scope was followed and checks pass, create one commit on
`main` using this file's exact filename (`001_setup.md`) as the commit message,
then push.

Do not commit or push partially completed work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
