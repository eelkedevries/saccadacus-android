# Using Saccadacus

Saccadacus records your eye- and head-movement data from the front camera while you use
your phone. All processing happens on the device; nothing is uploaded.

## Install

1. On your phone, open the project's **Releases → latest** page and download the `.apk`.
2. Tap the downloaded file and allow installation from your browser when prompted (you only
   need to do this once).

Each push to the project updates that page with a fresh build — re-download and install over
the top to update.

## First run

The first time you open the app, a short welcome screen explains what is recorded and which
permissions are needed. Tap **Got it** to continue. When you first press **Start tracking**,
the app shows a brief rationale and then asks for the **camera** permission (to see your
eyes) and the **notification** permission (to show the ongoing recording).

## Recording a session

1. *(Optional)* set a **profile** (Quality / Balanced / Battery), a **use-case** and **eye**
   selection, an optional **session name** and **note**, and the **Smoothing** / **Raw video**
   / **Overlay** toggles. These choices are remembered between launches.
2. *(Optional, recommended for long sessions)* tap **Allow background (battery)** to exempt the
   app from battery optimisation so it survives in the background.
3. Press **Start tracking**. A persistent notification appears and the system camera indicator
   stays on the whole time.
4. While running you can:
   - **Pause / Resume** — halt and continue the same session;
   - **Mark** — drop a timestamped marker into the data (e.g. to flag an event);
   - **Stop** — end the session.
5. After **Stop**, a short **summary** (saccade/fixation/blink counts and rates, durations,
   reliability) appears on screen.

The live readout shows frame rate, whether a face is detected, eye-local values, head pose,
event counts, lighting quality, and any storage/thermal warnings. The **Overlay** toggle draws
the tracked face mesh with the iris highlighted, so you can see tracking in real time.

## Getting your data out

- **Save session to Downloads** — drops the latest session's **whole bundle** into your phone's
  **Downloads** folder: the combined CSV plus its sidecars (`meta` with the calibration
  accuracy, `summary`, `sensors`, and any benchmark / frame-log / raw video). All files share
  the session's stamp. Open the Files app, or copy them off over USB. No share sheet needed.
- **Share session CSV** — sends the combined CSV via the Android share sheet (email, Drive, …).
- **Sessions** — lists every saved session with its name, note, size and date; from there you
  can Save the bundle to Downloads, Share, or Delete each one.

If the app is ever killed mid-session, the partial recording is recovered automatically on the
next launch and appears in the Sessions list.

## What the data means

See [`csv_schema.md`](csv_schema.md) for the CSV columns and sidecar files, and
[`privacy.md`](privacy.md) for what is and isn't recorded.
