package com.example.saccadacusandroid

import android.util.Size

/**
 * An operating profile (prompt 004): a target analysis resolution and an inference
 * cadence (run MediaPipe on every Nth analysed frame). Concrete fps/latency per profile
 * are measured by the benchmark, not assumed here.
 */
data class TrackingProfile(
    val name: String,
    val resolution: Size,
    val inferenceCadence: Int,
)

object ProbeConfig {
    val profiles: List<TrackingProfile> = listOf(
        TrackingProfile("Quality", Size(1280, 720), 1),
        TrackingProfile("Balanced", Size(640, 480), 2),
        TrackingProfile("Battery", Size(640, 480), 4),
    )

    @Volatile
    var selected: TrackingProfile = profiles[1]
}
