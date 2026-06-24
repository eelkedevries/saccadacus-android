# Saccadacus Android

*Saccadacus is an Android application.*

## What this is

Saccadacus is an Android application.

This project is built with the **android-kotlin + jetpack-compose** stack.

## Running locally

The phone is not used for compilation. Push to a branch and the **Build APK**
workflow builds a debug APK on a GitHub-hosted runner; download it from the
run's **Artifacts** and sideload it.

On a desktop with the Android SDK installed you can also build directly:

```bash
./gradlew assembleDebug --no-daemon
# output: app/build/outputs/apk/debug/app-debug.apk
```

## How it builds and deploys

Compilation is delegated to CI: every push builds a debug APK on a GitHub-hosted runner (no on-device build, no signing secrets), uploaded as the `app-debug` artefact to download and sideload. SDK levels and the JDK are template variables; library versions live in the Gradle version catalogue (`gradle/libs.versions.toml`). Release signing is deferred to a later, separately scoped prompt.

CI re-runs the build, the verify command, and a secret scan on every push, so a
broken or leaky commit is caught automatically.

## Development workflow

This repository follows the `eek-a-dev` commit-to-`main` workflow:

- one prompt equals one reviewable unit of work;
- prompt files live in `docs-dev/agent/prompts/`;
- prompt work is committed directly to `main` using the exact prompt filename as
  the commit message;
- do not create feature branches or pull requests unless explicitly instructed;
- run the project verify command and prompt checks before committing.

Start with `docs-dev/agent/how_to_use.md` for the daily workflow.

## Reference documents

- `docs-dev/reference/primary_authoritative/specification.md` — binding design and architecture reference.
- `docs-dev/reference/secondary_background/overview.md` — non-binding product overview.
- `docs-dev/planning/current_state.md` — current repository state and implemented progress.

## Local safety hooks

Optional but recommended after cloning locally:

```bash
pre-commit install
```

This enables local checks for large files, private keys, `.env` files, prompt
numbering, and basic formatting before commits.

## Licence status

No licence has been granted yet. All rights are reserved unless a `LICENSE` file
is added later.

## Public repository note

This repository is public. Do not commit secrets, credentials, private notes,
customer material, or proprietary material. `docs-dev/` is publicly visible but
is never included in the deployed build output.
