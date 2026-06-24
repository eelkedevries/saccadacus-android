# Overview

*Saccadacus Android* — non-binding orientation.

## What it is

Saccadacus Android is a native Android application that records **eye- and
head-movement data while the user interacts with another app**. The user opens
Saccadacus, picks a tracking mode, starts a session while the app is visible, then
switches to another app (a browser, PDF reader, Obsidian, or an experimental
interface). Front-camera frames continue to be processed **locally** through a
user-visible camera foreground service, and a persistent notification provides
pause/resume/stop. The user returns to Saccadacus to review and export the session.

It measures **derived signals** — left/right-eye position, iris-based tracking, blink
and saccade events, head orientation/position, device orientation, reliability,
tracking-loss periods, and timestamps. Raw camera footage is never stored unless the
user explicitly enables it.

## Stack

android-kotlin + jetpack-compose; CameraX; MediaPipe Tasks Vision Face Landmarker
(on-device, no Google Play Services); a Kotlin signal/event pipeline ported from the
existing browser implementation (`github.com/eelkedevries/saccadacus`); CSV export.

## Scope and platform

For researchers and self-experimenters measuring eye/head behaviour during natural
smartphone use, reading, or tasks run in another app. Built and distributed off-Play
(sideload) for research use; processing is fully on-device.

**Important boundary:** camera-relative eye movement does **not** by itself reveal which
screen element the user is looking at. Absolute *gaze mapping* and validated
*point-of-gaze* are distinct, harder problems and are out of scope for v1. Tracking
becomes unreliable when the device is not facing the user or the screen is off.

The binding design canon is `../primary_authoritative/specification.md`; the full
stack analysis is `../../planning/android_stack_research_report.md`.
