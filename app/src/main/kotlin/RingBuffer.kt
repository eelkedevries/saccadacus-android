package com.example.saccadacusandroid

/**
 * Pre-allocated float ring buffer for a continuous signal (prompt 006, spec §Domain
 * rules). Avoids per-sample allocation; oldest samples are overwritten once full.
 */
class FloatRingBuffer(val capacity: Int) {
    private val values = FloatArray(capacity)
    private var head = 0
    var size = 0
        private set

    fun add(value: Float) {
        values[head] = value
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /** [index] 0 = oldest retained sample. */
    operator fun get(index: Int): Float {
        require(index in 0 until size) { "index $index out of bounds (size $size)" }
        val start = (head - size + capacity) % capacity
        return values[(start + index) % capacity]
    }

    fun clear() {
        head = 0
        size = 0
    }

    /** Finite-difference velocity (units per second) between the two most recent samples. */
    fun latestVelocity(dtSeconds: Double): Double {
        if (size < 2 || dtSeconds <= 0.0) return 0.0
        return (get(size - 1) - get(size - 2)) / dtSeconds
    }
}
