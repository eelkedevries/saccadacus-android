# On-device gaze CNN (optional, side-loaded)

The app can run an optional appearance-based **gaze CNN** as a third signal source, alongside
**iris** (the default) and **blendshape**. It is fully additive: the existing tracking stays the
default, and the CNN **falls back to iris whenever no model is loaded**, so the app always works.

**No gaze model is shipped.** Every pretrained gaze model is trained on a non-commercial /
research-only dataset (GazeCapture, MPIIGaze, ETH-XGaze, Gaze360) whose licence forbids
redistributing trained weights, and this repository is public — so **you supply your own
`.tflite`** on the device for your own research. Nothing is committed and there is no build-time
download.

## Model contract

- **Location:** put one or more `*.tflite` files in the `gaze_models/` folder of the app's external
  files dir (`/Android/data/com.saccadacus.android/files/gaze_models/`). A single
  `gaze_model.tflite` in the parent folder is still honoured too.
- **Input:** `[1, 36, 60, 1]` float32 — a normalised single-eye patch, **grayscale and
  histogram-equalised**, values in `[0, 1]` (H = 36, W = 60). The app runs it per eye. The histogram
  equalisation matches the MPIIGaze eye-image contrast normalisation and also lifts low-light contrast,
  so train/convert your model to expect an equalised patch.
- **Output:** `[1, 2]` float32 = `(pitch, yaw)` in radians. The two eyes are averaged and the
  result feeds calibration → point-of-gaze, so any consistent units work (calibration absorbs the
  scale/convention).

The app **auto-detects a model's input profile** from its input tensor shapes at load (prompt 048). The
supported profile today is **EYE_GRAY** (the single `[1,36,60,1]` patch above); the CNN source only runs
a model whose profile it recognises, otherwise it falls back to iris. Further profiles (multi-input
models) are added as they are wired in.

A small MPIIGaze-style normalised-eye CNN matches this contract; train it on commercially-clean or
self-collected data (the standard appearance-based pipeline uses the eye-region crop plus the head
pose the app already extracts from the MediaPipe landmarks).

For a ranked catalogue of candidate models (suitable + excluded, with accuracy / licence / release
year and the per-model Saccadacus fit), see [`gaze_models.md`](gaze_models.md).

### Obtaining an MPIIGaze-style model

The official MPIIGaze checkpoint (`ptgaze`, MIT code) does **not** match this contract directly: it is
a **two-input** network (eye image **+** a 2-D *normalised* head-pose vector) and expects a full
data-normalisation warp (camera intrinsics + 3-D face model + `equalizeHist`), with its output
un-rotated by the per-frame normalising matrix. To run on this single-input contract, train (or
convert) a **single-input MPIIGaze-*style* eye CNN** that takes the equalised `[1,36,60,1]` patch and
outputs `[1,2]` pitch/yaw. The faithful two-input + warp path (to reuse the official weights) is a
larger, separately-scoped follow-up — see [`gaze_models.md`](gaze_models.md).

## How to side-load models

Push one or more models into the `gaze_models/` folder — e.g. over USB:

```
adb push mpiigaze.tflite  /sdcard/Android/data/com.saccadacus.android/files/gaze_models/
adb push blazegaze.tflite /sdcard/Android/data/com.saccadacus.android/files/gaze_models/
```

(or copy them there with a file manager). The selected model loads at session start; if none is
present the CNN source simply falls back to iris.

## Selecting a model and comparing (A/B/C)

Tap **Gaze source** to cycle iris → blendshape → cnn. When the source is **cnn**, a **Model: …**
button appears that cycles through the side-loaded models. To compare models (and against iris):

1. Calibrate and note the on-screen **"Mean check error"** (also `calibration_error` in
   `meta_<stamp>.csv`); the CNN inference latency (mean / p50 / p95 ms) is in `benchmark_<stamp>.csv`.
2. Switch model (or source) and calibrate again. Each recording's `meta_<stamp>.csv` records the
   active `signal_source` and `gaze_model`, so you can line up accuracy *and* latency per model.

Lower check-error is better. The active source is also recorded as `tracking_mode` in the combined CSV.
