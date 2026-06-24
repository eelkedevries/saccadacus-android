package com.example.saccadacusandroid

/**
 * User-selected session options chosen before Start (prompt 009): the use-case mode and
 * the eye-output selection. Recorded into the session metadata sidecar. The signal mode
 * is fixed to iris in v1 (spec §Domain rules).
 */
object SessionConfig {
    val useCaseModes = listOf("Reading", "Experimental", "Natural")
    val eyeModes = listOf("Left", "Right", "Binocular", "Both")

    @Volatile
    var useCaseMode: String = useCaseModes.first()

    @Volatile
    var eyeMode: String = "Binocular"

    /** Optional local raw-video recording (prompt 010). Off by default; explicit opt-in. */
    @Volatile
    var rawVideoEnabled: Boolean = false
}
