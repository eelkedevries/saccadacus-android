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
supported profiles are **EYE_GRAY** (the single `[1,36,60,1]` patch above), **WEB_EYE_TRACK**,
**DUAL_EYE_POG**, and **FULL_FACE** (all below); the CNN source only runs a model whose profile it
recognises, otherwise it falls back to iris. Further profiles are added as they are wired in.

### WebEyeTrack / BlazeGaze profile (multi-input)

`WEB_EYE_TRACK` — RedForestAI's MIT-licensed BlazeGaze (arXiv:2508.19544). Detected when the model has
**three inputs**: `image` `[1,128,512,3]` (RGB both-eyes strip, ÷255), `head_vector` `[1,3]` (unit head
direction), `face_origin_3d` `[1,3]` (eye origin in cm). Output `[1,2]` is a **point-of-gaze in
normalised screen `[-0.5,0.5]`**, fed through the app's calibration (which stands in for the model's
few-shot MAML personalisation). The app reproduces the eye-strip homography warp, the head vector, and a
best-effort metric origin from the MediaPipe landmarks + head pose.

**Convert to TFLite** (no weights are committed — supply your own; BlazeGaze's weights are
MPIIFaceGaze-trained, research-only):

```python
# from the WebEyeTrack repo: python/webeyetrack/model_weights/blazegaze_mpiifacegaze.keras
import tensorflow as tf
m = tf.keras.models.load_model("blazegaze_mpiifacegaze.keras", compile=False)
open("webeyetrack.tflite", "wb").write(tf.lite.TFLiteConverter.from_keras_model(m).convert())
# keep the input order [image, head_vector, face_origin_3d]; push the file to gaze_models/
```

Caveats: the `head_vector` Euler source and the `face_origin_3d` reconstruction are approximate (we lack
true camera intrinsics and the full metric face mesh) — verify on-device via the calibration error.

### Open Gaze / Google PoG family profile (multi-input)

`DUAL_EYE_POG` — the SAGE / iTracker-style point-of-gaze model (catalogue #7/#8/#9). Detected when the
model has **three inputs**: `leftEye` and `rightEye` `[1,128,128,3]` (RGB eye crops, NHWC) plus `lms`
`[1,8]` (eye-corner coordinates). The app builds each eye crop (the **left horizontally flipped**),
normalises per channel `(x/255 - mean)/std` with mean `(0.3741,0.4076,0.5425)` and the small std
`(0.02,0.02,0.02)`, and the 8 normalised eye-corner coordinates. Output `[1,2]` is a point-of-gaze in
**centimetres** (camera origin), fed through calibration (standing in for the model's per-user SVR/affine
personalisation). Inputs must be in order `[leftEye, rightEye, lms]`, NHWC.

**Licensing reality (important):** the named "Open Gaze" paper (arXiv:2308.13495) was **withdrawn** and
ships **no code or weights**. Its runnable twin `DSSR2/gaze-track` (GSoC 2021) has **no licence**
(all-rights-reserved) and GazeCapture-trained checkpoints (research-only). So **no weights are committed
and none can be** — train or obtain your own. To convert a model you have the rights to:

```
# PyTorch .ckpt (3 inputs: leftEye, rightEye, lms) -> ONNX -> NHWC TFLite
torch.onnx.export(model.eval(), (leye, reye, lms), "opengaze.onnx", opset_version=17)
# then: onnx2tf -i opengaze.onnx -o tf   (NHWC);  convert tf -> opengaze.tflite; push to gaze_models/
```

Caveats: the eye-corner / flip / left-right conventions are best-effort vs gaze-track — if gaze reads
mirrored, swap the eye inputs; verify on-device via the calibration error.

### Full-face profile (single-input)

`FULL_FACE` — a single `[1,224,224,3]` RGB face crop → `[1,2]` gaze angles. Serves **UniGaze-B**,
ETH-XGaze, MobileGaze and L2CS-Net. The app crops the face bounding box, resizes to 224×224, and
ImageNet-normalises per channel (mean `(0.485,0.456,0.406)`, std `(0.229,0.224,0.225)`), NHWC; the
`[1,2]` output (pitch/yaw or yaw/pitch — the order is absorbed by calibration) feeds the gaze signal.

**Important caveats:**
- This is a **plain landmark face box, not the ETH-XGaze data-normalisation warp** these models are
  trained on (solvePnP head pose + `warpPerspective` + un-rotation). Accuracy is therefore reduced vs
  the papers (UniGaze ablates this) — calibration recovers some of it; the faithful warp is a
  documented future step.
- **UniGaze:** only the **-B** size (~86 MB, ~45 MB int8) is on-device-viable — **-L/-H (0.6–1.3 GB)
  will not run on a phone**. Its licence is **non-commercial** (ModelGo MG-BY-NC, NC even after
  conversion) and the weights are research-only — supply your own; nothing is committed.

**Convert to TFLite** (NHWC; supply your own weights):

```
# PyTorch -> ONNX -> NHWC TFLite (UniGaze-B; or use Google's ai-edge-torch directly)
torch.onnx.export(model.eval(), face_224, "unigaze_b.onnx", opset_version=17)
# onnx2tf -i unigaze_b.onnx -o tf  (NHWC) ; convert tf -> unigaze_b.tflite ; push to gaze_models/
```

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
