# Privacy & data handling

Saccadacus is built to keep your data on your device and under your control.

## What is recorded

- **Derived movement signals only, by default:** per-frame eye-local iris position, head pose,
  blink state, and the detected saccade/blink/fixation events — all numbers, written to CSV.
- **Motion sensors:** rotation-vector / gyroscope / accelerometer samples, to relate head
  movement to the camera signal.
- **Session metadata** you provide (name, note) and your chosen settings.

## What is NOT recorded

- **No images or video by default.** The camera frames are analysed in memory and discarded;
  only the derived numbers are stored. Raw video is recorded **only** if you explicitly turn on
  the **Raw video** option for a session, and the recording state is shown the whole time.
- **No audio.**
- **No identity, contacts, or location.**

## On-device only

All processing — face landmarking, signal extraction, event detection — runs locally using an
on-device model. **Nothing is uploaded.** The app makes no network requests with your data.

## Where your data lives

Recordings are stored in the app's private external files directory on your device. They are
visible to you through the in-app **Sessions** screen, and you can copy a CSV into your public
**Downloads** folder with **Save CSV to Downloads** or send it with **Share**.

## When recording happens

Tracking is always **user-initiated while the app is visible**. It never starts silently: a
foreground-service notification and the system camera indicator are shown for the entire
recording, and you can Pause or Stop at any time.

## Deleting your data

Open **Sessions** and use **Delete** on any recording (with confirmation). Uninstalling the app
removes all of its stored data. Any raw video you opted into is stored in the same private
directory and is removed the same way.

## Distribution

The app is distributed off-store (sideloaded from the Releases page) for research use; it is not
published to an app store, and it is not signed for store distribution.
