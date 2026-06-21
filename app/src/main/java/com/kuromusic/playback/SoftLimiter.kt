package com.kuromusic.playback

import kotlin.math.abs
import kotlin.math.exp

class SoftLimiter(
    private val sampleRate: Int = 48000
) {
    private val ceiling = 0.891f // -1 dB

    private val attackCoeff = 1.0f - exp(-1.0f / (sampleRate * 0.005f))
    private val releaseCoeff = 1.0f - exp(-1.0f / (sampleRate * 0.050f))

    private var gain = 1f

    fun process(sample: Float): Float {
        val abs = abs(sample)
        val target = if (abs * gain > ceiling) {
            ceiling / (abs + 1e-6f)
        } else 1f

        gain += (target - gain) * if (target < gain) attackCoeff else releaseCoeff

        return sample * gain
    }

    fun reset() {
        gain = 1f
    }
}
