# Gaze-model catalogue (candidate on-device models)

A durable record of the appearance-based gaze-estimation models reviewed for Saccadacus: the
**suitable** shortlist (ranked) and the **excluded** ones (with reasons). It complements the
side-load guide in [`gaze_cnn.md`](gaze_cnn.md) and mirrors the in-app *"Gaze models — compare"*
fold-out (`GazeModels.kt`). The in-app panel is the on-device snapshot; this file is the fuller
decision record.

> **Reviewed:** 2026-06. Accuracy / release year / last-update values are best-effort from the
> project's research scout — **verify upstream** before relying on them. Figures use each model's
> native metric: **cm** = on-screen point-of-gaze error (phone/tablet); **deg** = 3-D gaze-direction
> angular error. They are not directly comparable across the two metrics.

## What "suitable" means here

A model is on the shortlist only if it clears all three:

1. **Runs on-device on Android** (or converts to a mobile runtime — TFLite/LiteRT/ONNX). No
   browser-only or desktop-only toolkits.
2. **Actually outputs gaze** — a direction or a point-of-regard — not just landmarks or a dataset.
3. **Code licence permits at least research side-loading.**

## Three caveats apply to every row — no exceptions

1. **Weights are research-only / dataset-tainted.** They are trained on non-commercial datasets
   (GazeCapture, MPIIGaze, ETH-XGaze, Gaze360 …) whose terms forbid redistributing derived weights.
   The **`code licence`** column is the *code* licence only. **No weights are committed to this public
   repo** — you supply your own `.tflite` on the device for your own research.
2. **None ships as a ready `.tflite`.** Each needs a conversion or training step.
3. **Only the *eye-gray* input profile exists in the app today** (prompts 040–043: a single-eye
   `[1,36,60,1]` histogram-equalised grayscale patch → `[1,2]` pitch/yaw; equalisation added in 047).
   Models needing a different input ("new profile" below) need that plumbing built first.

## Suitable models (ranked by how close to shipping on Saccadacus)

| # | Model | Publisher | Input → output | Accuracy | Released | Last update | Weights | Code licence | Verdict |
|---|-------|-----------|----------------|----------|----------|-------------|---------|--------------|---------|
| 1 | **MPIIGaze** (ptgaze) | MPI-Inf; impl. *hysts* | eye 36×60 gray **+ 2-D head pose** → pitch/yaw | ~4.5–6 deg | 2015 (data) / impl. ~2021 | ~2021 | `.pth` | **MIT** (code) | Canonical eye CNN. **Two-input** + needs a data-normalisation warp — *not* a drop-in on today's single-input contract (see note). Closest target; start here. |
| 2 | **WebEyeTrack / BlazeGaze** | RedForestAI (Vanderbilt) | eye RGB + head pose → PoG | ~2.0–2.3 cm | 2025 | 2025 | TF/TFJS, 0.67 MB | **MIT** | Closest to our *full* stack (MediaPipe + patch + head pose + few-shot); tiny. **Wired in (prompt 049)** as the `WEB_EYE_TRACK` profile (arXiv:2508.19544). |
| 3 | **MobileGaze** (yakhyo) | indie (Y. Kholmatov) | full-face → yaw/pitch | ~11–13 deg (Gaze360) | ~2024 | ~2024 | `.pt`, 4.8 MB | **MIT** | Best on-device speed/size; weights downloadable. Needs a full-face profile. |
| 4 | **L2CS-Net** | Abdelrahman et al. | full-face → binned yaw/pitch | 3.92 deg (MPIIGaze) | 2022 | ~2023 | `.pkl`/ONNX, ~96 MB | **MIT** | Most accurate here; pre-converted ONNX exists. Heavy ResNet-50; full-face. |
| 5 | **GazeML / ELG** | Park et al. (ETH) | eye gray → landmarks → gaze | modest | 2018 | ~2019 | TF1 ckpt | **MIT** | Cleanest licence (synthetic UnityEyes); eye-sized. TF1-era; indirect gaze. |
| 6 | **ETH-XGaze** | Zhang et al. (ETH) | norm. face → yaw/pitch | ~4–4.5 deg | 2020 | ~2021 | ResNet-50, ~90 MB | **CC BY-NC-SA** (code!) | Strong head-pose normalisation; documented. Code *and* data non-commercial; heavy. |
| 7 | **Gaze-Track** (s0mnaths, GSoC) | s0mnaths | eyes + landmarks → PoG | ~1.0–2.0 cm | ~2021 | ~2021 | TF ckpt | none stated | Mobile recreation of Google's paper; cm metric like ours. No licence; no `.tflite`. |
| 8 | **iTracker / GazeCapture** | MIT CSAIL | 2 eyes + face + grid → PoG | ~1.3–2.0 cm | 2016 | ~2016 | Caffe | research-only | The original mobile-gaze benchmark. Dated (Caffe); multi-input. |
| 9 | **Open Gaze** (replication) | open repl. (arXiv 2308.13495) | eye crops + landmarks → PoG | ~1.9 cm | 2023 | ~2023 | repo weights | research (repo) | Open clone of Google's 0.46 cm approach; worse than the original. **Profile wired (prompt 050)** as `DUAL_EYE_POG` (also serves #7/#8); arXiv 2308.13495 is **withdrawn** — supply your own weights. |
| 10 | **FAZE** | NVlabs | norm. eye → gaze (few-shot) | 3.18 deg @ 3-shot | 2019 | ~2020 | weights | **NVIDIA SCL** (non-comm.) | Best personalisation idea (MAML). Hard NC licence; complex. |
| 11 | **UniGaze** | ut-vision | full-face ViT → yaw/pitch | SOTA cross-dataset | 2025 | 2025 | ViT (large) | **ModelGo MG-BY-NC** | Best generalisation across datasets. **Profile wired (prompt 051)** as `FULL_FACE`; only **-B** is on-device-viable (L/H won't run); explicitly NC. |
| 12 | **RT-GENE / RT-BENE** | Fischer et al. (Imperial College) | full-face + eyes → 3-D gaze + blink | ~7.7 deg (in-the-wild) | 2018 / 2019 | ~2020 | VGG, 4-net ensemble | **CC BY-NC-SA** | Research; heaviest (ensemble). **RT-BENE is a useful blink reference.** |

### MPIIGaze — the contract gap (why #1 is not a drop-in)

The pretrained `mpiigaze_resnet_preact.pth` from `ptgaze` (`hysts/pytorch_mpiigaze_demo`, MIT code):

- is a **two-input** network — `forward(eye_image, head_pose2d)` — concatenating a 2-D normalised
  head-pose vector just before the final FC layer;
- expects the eye crop to come from a **data-normalisation warp** (camera intrinsics + a 3-D face
  model + a per-frame *normalising rotation*, `cv2.warpPerspective` to 60×36), followed by
  **`cv2.equalizeHist`**, then `ToTensor` (÷255, no mean/std);
- outputs `(pitch, yaw)` in radians in **normalised** space, which must be turned into a 3-D vector
  and **un-rotated by the per-frame normalising matrix** to recover real-world gaze.

Our current eye-gray profile feeds a single-input, axis-aligned, histogram-equalised crop. So #1 can be
approached in increments: (a) histogram-equalised eye patch (prompt 047, matches the eye-image step),
then (b) the full faithful path — 2-D head-pose input + normalisation warp + un-rotation — to use the
official weights. Until (b), the contract targets a **single-input MPIIGaze-*style* eye CNN you train
to the contract**, not the official checkpoint.

## Commercial (separate category — not open)

- **SeeSo (VisualCamp)** — closed on-device SDK with real, calibrated accuracy, but needs a licence key
  (manage.seeso.io) and runs against Saccadacus's open / privacy-first ethos. Listed for completeness;
  not recommended.

## Excluded (considered and rejected)

| Model | Why excluded |
|-------|--------------|
| **TabletGaze** | 2016 HoG + Random-Forest (not deep); modest; classic baseline only. |
| **OpenFace 2.0** (gaze) | Desktop C++; bespoke CMU academic-only (paid commercial) licence; dataset-tainted. |
| **OpenGaze** | Requires OpenFace (NC); desktop; research toolkit. |
| **GAZEL** | No released weights — an Android TFLite *scaffold* (train-your-own); some examples pull Firebase/Play Services. |
| **WalidAlHassan/Gaze-Estimation** | Gated repo, no metadata/files — not usable. |
| **MediaPipe Iris** | Outputs landmarks, **not gaze** — it is the building block we already use. |
| **Google smartphone model** (Nature Comms 2020) | 0.46 cm — the accuracy ceiling — but **never released**; no code/weights. |
| **WebGazer.js** | GPL; browser-only; effectively unmaintained. |
| **GazeTracking** (antoinelame) | Desktop webcam demo; no point-of-regard output. |
| **Gaze360** | A research **dataset** (+ baseline ~11 deg), not a deployable model. |
| **Pupil Core** | IR head-mounted hardware pipeline — not front-camera. |

## Integration status & unlock order

Today the app can A/B only the **eye-gray** profile (the CNN path of prompts 040–044). The build-out
order that unlocks the most of the shortlist:

1. **eye-gray** *(exists)* — single-input `[1,36,60,1]` → `[1,2]`. Refined to a histogram-equalised
   patch in prompt 047. Targets a single-input MPIIGaze-style CNN.
2. **eye-gray + 2-D head pose + normalisation warp** — unlocks the **faithful MPIIGaze** (#1) using the
   official weights.
3. **full-face** → yaw/pitch — **wired in prompt 051 as `FULL_FACE`** (serves #3, #4, #6, #11; uses a
   plain landmark face box — the ETH-XGaze data-normalisation warp for best accuracy is a documented
   follow-up).
4. **multi-input point-of-gaze** (eyes + face + landmarks/grid) — unlocks **#7, #8, #9**.
5. **few-shot personalisation** — **#10**.
