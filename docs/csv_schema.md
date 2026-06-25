# Data format

A session produces one **combined CSV** plus a few **sidecar** files, all in the app's
private external files directory. Saving a session to Downloads exports the whole set together
(the combined CSV and every sidecar sharing its stamp). Numbers use a locale-independent format
(`.` decimal separator, fixed decimals).

## Clock

The canonical timestamp is `elapsed_realtime_nanos` — Android's monotonic
`SystemClock.elapsedRealtimeNanos()`, which does not jump when the wall clock changes. Each
row also carries `wallclock_anchor_ms`, a one-off pairing of that monotonic clock with
`System.currentTimeMillis()` captured at session start, so the data can be related to wall-clock
time and aligned across apps/devices.

## Combined CSV — `session_<stamp>[_<name>].csv`

One row per `row_type`. Columns (in order):

| Column | Meaning |
| --- | --- |
| `elapsed_realtime_nanos` | Canonical monotonic timestamp (ns). |
| `camera_sensor_timestamp` | Camera frame timestamp (ns), source-dependent. |
| `camera_timestamp_source` | Origin of the camera timestamp, if known. |
| `wallclock_anchor_ms` | Wall-clock anchor for the session (ms since epoch). |
| `row_type` | `time_series`, `event`, or `task`. |
| `tracking_mode` | Signal mode (`iris` in v1). |
| `eye_selection_mode` | Left / Right / Binocular / Both. |
| `left_eye_x_local`, `left_eye_y_local` | Participant left eye iris position, eye-width units (x = participant right, y = up). |
| `right_eye_x_local`, `right_eye_y_local` | Participant right eye iris position. |
| `binocular_x_local`, `binocular_y_local` | Combined eye position (when applicable). |
| `left_eye_reliability`, `right_eye_reliability` | Per-eye reliability 0–1. |
| `iris_reliability`, `pupil_reliability` | Iris reliability; `pupil_*` is unused in v1. |
| `head_yaw`, `head_pitch`, `head_roll` | Head orientation (degrees). |
| `head_translation_x/y/z` | Head translation (when available). |
| `blink_state` | Per-eye blink state, `left|right` (open/closing/closed/opening/unknown). |
| `event_type` | For `event` rows: `saccade`, `blink`, or `fixation`. |
| `event_onset`, `event_offset`, `event_duration` | Event timing (ms, on the canonical clock). |
| `event_direction` | Event direction, when applicable. |
| `event_relative_amplitude` | Saccade amplitude (eye-width units). |
| `event_confidence` | Event confidence 0–1. |
| `event_head_motion_label` | `saccade_head_still` / `saccade_during_head_movement` / `uncertain_head_motion`. |
| `task_marker` | Label for a `task` (interaction marker) row. |
| `annotation` | Free text; also carries a fixation `centroid=x;y`. |
| `gaze_screen_x`, `gaze_screen_y` | Calibrated, normalised (0–1) point-of-gaze; empty when uncalibrated. |

`tracking_mode` is `iris` or `blendshape` (the gaze source). `gaze_screen_x/y` are populated
only after calibration. `pupil_*` columns exist but are never populated in v1 (iris-centre
only). Raw video is never written unless explicitly enabled.

### Row types

- **`time_series`** — one per analysed frame: the eye-local, head-pose and blink columns.
- **`event`** — detected `saccade`, `blink`, or `fixation`, using the `event_*` columns.
- **`task`** — a user **Mark**, with an optional label in `task_marker`.

## Sidecar files

- **`meta_<stamp>.csv`** (`key,value`) — session metadata: profile, session name/note,
  use-case and eye mode, raw-video flag/path, smoothing (`filter_enabled`/`filter_alpha`),
  and start/stop wall-clock and sample count.
- **`sensors_<stamp>.csv`** (`sensor,timestamp_nanos,v0,v1,v2`) — rotation-vector / gyroscope /
  accelerometer samples, in their own timestamp domain.
- **`summary_<stamp>.csv`** (`key,value`) — per-session aggregates: duration; counts and
  per-minute rates of saccades/fixations/blinks; mean+median saccade amplitude and duration;
  mean fixation duration; mean reliability; total tracking-loss time.
- **`benchmark_<stamp>.csv`** — on-device inference benchmark (fps, latency percentiles).
- **`frame_log_<stamp>.csv`** — raw per-frame timing (`frame_index, camera_sensor_timestamp,
  elapsed_realtime_nanos`).
- **`video_<stamp>.mp4`** — raw front-camera video, only when explicitly enabled (off by default).

Every file from one session shares the same `<stamp>`, so the combined CSV and its sidecars
travel together when you save the session to Downloads.
