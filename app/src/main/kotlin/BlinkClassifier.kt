package com.example.saccadacusandroid

/**
 * Per-eye blink-state classification (prompt 034).
 *
 * MediaPipe's `eyeBlink` blendshape has a person- and camera-angle-dependent baseline for a
 * fully *open* eye — an on-device daylight recording showed an open eye sitting at 0.2–0.5, so
 * the old fixed thresholds (`>= 0.2` "closing", `>= 0.5` "closed") mislabelled most open frames
 * as "closing" and produced multi-second false blinks. This tracks each eye's open baseline
 * adaptively and classifies *relative* to it: only a rise well above the running baseline counts
 * as a closing/closed eye. The baseline updates only while the eye reads open, so a blink cannot
 * inflate it, and is clamped so it cannot drift high enough to swallow a genuine blink.
 *
 * Stateful and single-threaded: one instance per frame pipeline; call [reset] when the face is
 * lost so a new tracking run warms up afresh.
 */
class BlinkClassifier {
    private var leftBaseline = Float.NaN
    private var rightBaseline = Float.NaN

    fun classifyLeft(score: Float): BlinkState {
        val (state, base) = classify(score, leftBaseline)
        leftBaseline = base
        return state
    }

    fun classifyRight(score: Float): BlinkState {
        val (state, base) = classify(score, rightBaseline)
        rightBaseline = base
        return state
    }

    fun reset() {
        leftBaseline = Float.NaN
        rightBaseline = Float.NaN
    }

    private fun classify(score: Float, baseline: Float): Pair<BlinkState, Float> {
        // Warm-up: adopt the first score as the open baseline and read OPEN for that frame.
        if (baseline.isNaN()) return Pair(BlinkState.OPEN, score)
        val state = when {
            score >= baseline + CLOSED_DELTA -> BlinkState.CLOSED
            score >= baseline + CLOSING_DELTA -> BlinkState.CLOSING
            else -> BlinkState.OPEN
        }
        // Adapt only while open so a blink spike cannot raise the baseline; clamp so a real
        // blink stays reachable above the baseline.
        val nextBaseline = if (state == BlinkState.OPEN) {
            (baseline + BASELINE_ALPHA * (score - baseline)).coerceIn(0f, BASELINE_MAX)
        } else {
            baseline
        }
        return Pair(state, nextBaseline)
    }

    companion object {
        const val CLOSING_DELTA = 0.25f  // rise above the open baseline to count as closing
        const val CLOSED_DELTA = 0.5f    // rise above the open baseline to count as closed
        const val BASELINE_ALPHA = 0.1f  // EMA weight; tracks the open eye, not blinks
        const val BASELINE_MAX = 0.45f   // cap so the baseline cannot swallow a real blink
    }
}
