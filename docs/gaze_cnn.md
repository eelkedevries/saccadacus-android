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

- **File:** `gaze_model.tflite`, placed in the app's external files dir
  (`/Android/data/com.saccadacus.android/files/gaze_model.tflite`).
- **Input:** `[1, 36, 60, 1]` float32 — a normalised single-eye patch, grayscale, values in
  `[0, 1]` (H = 36, W = 60). The app runs it per eye.
- **Output:** `[1, 2]` float32 = `(pitch, yaw)` in radians. The two eyes are averaged and the
  result feeds calibration → point-of-gaze, so any consistent units work (calibration absorbs the
  scale/convention).

A small MPIIGaze-style normalised-eye CNN matches this contract; train it on commercially-clean or
self-collected data (the standard appearance-based pipeline uses the eye-region crop plus the head
pose the app already extracts from the MediaPipe landmarks).

## How to side-load a model

Place your `gaze_model.tflite` at the path above — e.g. over USB:

```
adb push gaze_model.tflite /sdcard/Android/data/com.saccadacus.android/files/gaze_model.tflite
```

(or copy it there with a file manager). The model loads at session start; if it is absent the CNN
source simply falls back to iris.

## Selecting it and comparing accuracy (A/B)

Tap **Gaze source** to cycle iris → blendshape → cnn. With a model loaded, the CNN drives the gaze.
To compare it against iris:

1. Calibrate on **iris** and note the on-screen **"Mean check error"** (also `calibration_error`
   in `meta_<stamp>.csv`).
2. Switch to **cnn**, calibrate again, and compare. Lower is better.

The CNN's inference latency (mean / p50 / p95 ms) is written to `benchmark_<stamp>.csv` while the
CNN source is active, so you can confirm it fits your frame budget on your device. The active
source is recorded as `signal_source` (meta) and `tracking_mode` (combined CSV).
