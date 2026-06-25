# On-device gaze CNN — model contract (prompt 040)

The app can optionally run a side-loaded gaze CNN as a third signal source, alongside iris and
blendshape. No gaze model has redistributable weights — every pretrained gaze model is trained on a
non-commercial / research-only dataset (GazeCapture, MPIIGaze, ETH-XGaze, Gaze360), and several
explicitly forbid "models trained on the dataset" — so **no model is committed to this (public)
repo and there is no build-time download.** The user supplies their own `.tflite` on the device for
their own research; the repo stays redistribution-clean.

## Side-load location

`<app external files dir>/gaze_model.tflite` — i.e. `getExternalFilesDir(null)/gaze_model.tflite`
(`/Android/data/com.saccadacus.android/files/gaze_model.tflite`). The file is never committed. If it
is absent, the CNN source is simply unavailable and tracking stays on iris (`GazeCnn.isAvailable` is
false).

## Tensor contract

- **Input:** `[1, 36, 60, 1]` float32 — a normalised **single-eye patch**, grayscale, values in
  `[0, 1]` (H = 36, W = 60). Run per eye.
- **Output:** `[1, 2]` float32 = `(pitch, yaw)` in radians (gaze direction). The two eyes are
  averaged and the result feeds the existing calibration → point-of-gaze, so any consistent units
  work — calibration absorbs the scale/convention.

## Runtime

Standalone TensorFlow Lite / LiteRT (`org.tensorflow:tensorflow-lite`), CPU / XNNPACK by default
(no Google Play Services; NNAPI is deprecated). Preprocessing (prompt 041), inference + signal-source
wiring (042), and the benchmark / validation / side-load how-to (043) build on this loader.
