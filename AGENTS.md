# Agent instructions

Concise root rules for Claude Code and Codex. Detailed procedures live in the guides under `docs-dev/agent/` — start with `docs-dev/agent/how_to_use.md`. These rules guide behaviour but do not enforce it; safety also relies on checks, hooks, and review.

## Core principles

- One prompt = one reviewable unit. Do only what the prompt or request says; prefer the smallest correct change. No unrequested features, frameworks, tests, or architecture.
- Before editing, briefly state intended files, planned changes, and out-of-scope items.

## Turning intent into prompts

Prompts need not be hand-written. When the user describes a task in plain language, draft the prompt file for them, show the draft, and run it only on approval. For a large or vague goal, first propose a numbered sequence of small prompts and get approval before running any. `scripts/new-prompt.sh <short_name>` scaffolds a prompt manually if preferred.

## Where things live

- `docs-dev/agent/prompts/` — numbered prompt files (work queue and audit trail).
- `docs-dev/agent/` — guides: `how_to_use`, `prompt_authoring_guide`, `prompt_execution_guide`, `prompt_iteration_guide`, `document_contract`, `gpt_author_instructions`.
- `docs-dev/reference/primary_authoritative/specification.md` — binding canon (ground truth).
- `docs-dev/reference/secondary_background/` — non-binding context and examples.
- `docs-dev/planning/current_state.md` — living "where we are"; read at session start.
- `docs/` — user-facing, publishable docs.

If the repository is public, everything in it — including `docs-dev/` — is publicly visible. Never put secrets, tokens, or `.env*` files in the repository. `docs-dev/` must never reach the deployed build output (see `scripts/check-public-build.sh`).

## Ground truth

Treat `primary_authoritative/specification.md` as correct. Never contradict it; if a change would conflict, stop and flag it. Empty or stubbed sections mean "not yet decided" — no constraint. When a decision changes, update the spec and bump its version. `secondary_background/` is informational only.

Update `current_state.md` when a prompt adds a system or a key decision — for genuinely useful orientation, not after routine commits.

## Running and committing (commit-to-`main`)

Default workflow (see `prompt_execution_guide.md` for the full steps):

- Run a prompt: clean tree on `main` → make only that change → run the verify command (Project conventions) and the prompt's checks → if they pass, commit to `main` and push. One commit per prompt; no branch, no PR.
- **Commit messages:** prompt work uses the exact prompt filename; any other commit uses a conventional prefix (`feat:`, `fix:`, `docs:`, `refactor:`).
- Do not commit partial or failing work unless a WIP commit is requested. Do not rewrite history or force-push.

## Project conventions

Recorded at setup; these override the defaults above where they differ.

- **Language / locale:** British English in code, comments, and user-facing text.
- **Workflow:** commit and push directly to `main`; no feature branches or pull requests unless explicitly requested.
- **Verify command:** `./gradlew assembleDebug --no-daemon` (CI builds the APK on a hosted runner).
- **Testing policy:** no tests unless a prompt asks for them.
- **Deploy base path:** _not applicable (no static site)._


## Android stack notes

This project compiles in CI, not on the phone. Keep these invariants:

- **The Gradle wrapper stays committed** — `gradle/wrapper/gradle-wrapper.jar` and the executable bit on `gradlew` (restore with `git update-index --chmod=+x gradlew` if it is ever lost). A missing wrapper jar or lost executable bit are the two routine causes of a first CI build failing.
- **Builds go through CI by default.** Every push runs the **Build APK** workflow, which produces the debug `app-debug` artefact on a GitHub-hosted runner. The debug signing key suffices for sideloading.
- **Release signing, keystores, and Play/Firebase distribution are out of scope** here; they belong to a later, separately scoped prompt.
- **SDK levels and the JDK are template variables**; library and plugin versions live in the Gradle version catalogue (`gradle/libs.versions.toml`). Bump versions there, not in the build scripts.
- **If on-device building is ever attempted** (for example Termux on the 8 GB phone), cap memory and skip the daemon — `./gradlew assembleDebug --no-daemon` with a capped heap (`org.gradle.jvmargs=-Xmx2048m` is already set) — and set Termux's `aapt2FromMavenOverride` to a native `aapt2`, since the Maven `aapt2` is an x86-64 binary that will not run on aarch64. On-device building is not the default path.

## Supporting guides

`how_to_use.md` (map + daily loop), `prompt_authoring_guide.md` (writing prompts), `prompt_execution_guide.md` (running them), `prompt_iteration_guide.md` (supersede/revert), `document_contract.md` (what documents the project expects), `reviews/code_review_guide.md` (reviews).

## Final response

Every response must end with this five-section report:

### 1. Work done
`Completed successfully`, `Partially completed`, or `Not completed` — and whether all acceptance criteria were met. Cite the commit hash and the result of the verify command / checks as evidence.

### 2. Files changed
Every file added, modified, or deleted, one line each. `None.` if no changes.

### 3. Scope deviations
Any work outside the prompt's `Required changes`, with a reason. `None.` if scope was followed exactly.

### 4. Open issues
List issues, or `None.`

### 5. Human actions required
List required actions, or `None. You can proceed to the next prompt.`
