package com.example.saccadacusandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live inference-benchmark summary (prompt 004). Latencies in milliseconds. */
data class BenchmarkSnapshot(
    val profileName: String = "",
    val analysedFrames: Long = 0L,
    val droppedFrames: Long = 0L,
    val analysedFps: Double = 0.0,
    val latencyMeanMs: Double = 0.0,
    val latencyP50Ms: Double = 0.0,
    val latencyP95Ms: Double = 0.0,
)

object BenchmarkStats {
    private val _state = MutableStateFlow(BenchmarkSnapshot())
    val state: StateFlow<BenchmarkSnapshot> = _state.asStateFlow()

    fun reset(profileName: String) {
        _state.value = BenchmarkSnapshot(profileName = profileName)
    }

    fun update(snapshot: BenchmarkSnapshot) {
        _state.value = snapshot
    }
}
