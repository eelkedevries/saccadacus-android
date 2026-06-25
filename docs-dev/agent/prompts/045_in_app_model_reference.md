# Task: In-app gaze-model comparison fold-out

## Goal

Add a collapsible "Gaze models — compare" panel to the control screen that shows the known-models
comparison (name, publisher, licence, year, accuracy, size, pros, cons) so the user can browse and
compare candidate models on the device.

## Scope

Implement only the static reference data + a collapsible UI panel. It is informational — it does NOT
change the side-loaded-model selection, the gaze pipeline, or anything recorded. Additive UI only.

## Context

The scouting (this session) produced a ranked comparison of on-device gaze models; this surfaces it
in the app as a fold-out reference next to the gaze-source / model controls, so the user can decide
which model to obtain/side-load without leaving the app. It is a static snapshot (note that in the
panel).

## Required changes

1. Add a `GazeModels` reference: a `GazeModelInfo` data class (name, publisher, licence, year,
   accuracy, size, pros, cons) and a ranked `all: List<GazeModelInfo>` populated with the surfaced
   models (the ranked comparison from this session — all entries, including the unusable/no-weights
   ones, each with a short pro/con). Keep it pure (no Android types) so it is unit-testable.
2. Add a **collapsible** "Gaze models — compare (tap to show)" control on the control screen
   (default collapsed) that, when expanded, renders each model compactly: name (emphasised), then
   `publisher · licence · year`, `accuracy · size`, a `✓ pros` line and a `✗ cons` line. Include a
   one-line note that weights are research-only / a static snapshot.
3. Add a unit test asserting the reference list is non-empty and every entry has a name + licence.

## Do not implement

Do not:
- change the side-loaded-model discovery/selection (prompt 044), the gaze pipeline, or any recording;
- fetch the data at runtime (it is a static in-app snapshot);
- commit a model.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `testDebugUnitTest` pass in CI;
- the control screen shows a collapsible panel that lists the known models with their key facts +
  pros/cons; the reference data is unit-tested as non-empty/well-formed.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. This is currently the last queued prompt (`045`); there are no later prompts
unless earlier execution added some. If a `046+` exists when this runs, apply the same check to it.
Otherwise say nothing downstream is affected. Do not edit already-run prompts, do not renumber, do
not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`045_in_app_model_reference.md`) as the commit message, then push. Do not commit
partial work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
