package com.example.saccadacusandroid

/**
 * User-selected session options chosen before Start (prompt 009): the use-case mode and
 * the eye-output selection. Recorded into the session metadata sidecar. The signal mode
 * is fixed to iris in v1 (spec §Domain rules).
 */
object SessionConfig {
    val useCaseModes = listOf("Reading", "Experimental", "Natural")
    val eyeModes = listOf("Left", "Right", "Binocular", "Both")

    // Gaze signal source (prompt 030): iris-centre (default) or eye-look blendshapes.
    const val SOURCE_IRIS = "iris"
    const val SOURCE_BLENDSHAPE = "blendshape"
    const val SOURCE_CNN = "cnn" // side-loaded on-device gaze CNN (prompt 042)

    @Volatile
    var signalSource: String = SOURCE_IRIS

    // Selected side-loaded CNN model filename (prompt 044); empty = first available.
    @Volatile
    var gazeModelName: String = ""

    @Volatile
    var useCaseMode: String = useCaseModes.first()

    @Volatile
    var eyeMode: String = "Binocular"

    /** Optional local raw-video recording (prompt 010). Off by default; explicit opt-in. */
    @Volatile
    var rawVideoEnabled: Boolean = false

    /** Optional session name and free-text note (prompt 018); a blank name falls back to the timestamp. */
    @Volatile
    var sessionName: String = ""

    @Volatile
    var sessionNote: String = ""

    /** Optional eye-local smoothing (prompt 021); OFF by default so the default signal path is unchanged. */
    @Volatile
    var filterEnabled: Boolean = false

    @Volatile
    var filterAlpha: Float = 0.3f
}
