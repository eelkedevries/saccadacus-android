# Task: Gaze-model catalogue (repo record)

## Goal

Record, in the repo, the catalogue of candidate on-device gaze-estimation models reviewed this
session — the *suitable* ones (ranked) and the *excluded* ones (with reasons) — with enough metadata
to choose one: name, publisher, input→output, accuracy, release year, last update, weights format,
code licence, and a Saccadacus-fit verdict. This is the durable "remember these models" note.

## Scope

Documentation + the in-app reference list only. Add a markdown catalogue under `docs/`, cross-link it
from `docs/gaze_cnn.md`, and add the one missing suitable model (RT-GENE / RT-BENE) to the in-app
`GazeModels` reference so the two stay consistent. No pipeline, contract, or recording changes.

## Context

The session's scouting + adversarial evaluation produced a defensible shortlist of gaze models that
could run on Saccadacus's on-device, privacy-first, public-repo stack, plus a list of models that were
considered and rejected. The in-app fold-out (prompt 045) already shows a snapshot; this prompt writes
the authoritative, fuller catalogue (with accuracy / release year / last update) to the repo so the
decision record survives, and tops up the in-app list to match.

Honesty constraints to preserve in the catalogue:
- Every pretrained gaze model's **weights** are research-only / dataset-tainted (GazeCapture, MPIIGaze,
  ETH-XGaze, Gaze360 …); the `licence` column is the **code** licence. Weights are never committed.
- **None ships as `.tflite`** — each needs conversion/training.
- The official **MPIIGaze** model is a **two-input** network (eye image + 2-D normalised head pose) and
  relies on a full data-normalisation warp; it does **not** drop onto the current single-input
  `[1,36,60,1]` contract unchanged. Say so.

## Required changes

1. Add `docs/gaze_models.md`: the suitability filter (3 criteria), the universal caveats (above), a
   **ranked table of suitable models** with columns *#, model, publisher, input→output, accuracy,
   released, last update, weights, code licence, verdict*, a short **SeeSo** (commercial) note, and a
   **table of excluded models** each with a one-line reason. Date the review and note values are
   best-effort (verify upstream).
2. Cross-link the new catalogue from `docs/gaze_cnn.md` (one line).
3. Add the one missing suitable model — **RT-GENE / RT-BENE** — as a `GazeModelInfo` entry in
   `GazeModels.kt`, so the in-app reference matches the catalogue.

## Do not implement

Do not:
- change the gaze pipeline, the model contract, calibration, or any recording;
- fetch data at runtime;
- commit a model or add a build-time download.

## Acceptance criteria

The task is complete when:
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass in CI;
- `docs/gaze_models.md` lists the suitable models (ranked, with accuracy / release year / last update)
  and the excluded models with reasons; `docs/gaze_cnn.md` links to it; the in-app `GazeModels` list
  includes RT-GENE / RT-BENE.

## Checks

Run `./gradlew assembleDebug --no-daemon` and `./gradlew testDebugUnitTest --no-daemon` (via CI).

## Cross-prompt impact check

Prompts run in sequence. A follow-up `047` (MPIIGaze-style eye normalisation) is planned; it will
refine the eye-patch contract (histogram equalisation) and update `docs/gaze_cnn.md`. This prompt only
*adds* a catalogue + one in-app entry, so it does not invalidate `047`; `047` should keep the catalogue
consistent if it changes the contract wording. Do not edit already-run prompts, do not renumber, do
not expand scope.

## Commit and push

If and only if the scope was followed and checks pass, create one commit on `main` using this file's
exact filename (`046_gaze_models_catalogue.md`) as the commit message, then push. Do not commit partial
work unless explicitly instructed.

## Final report

End with the required final report specified in `AGENTS.md`.
