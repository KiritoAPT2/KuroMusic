package com.kuromusic.playback

import kotlin.math.ln
import kotlin.math.sqrt

data class LoudnessStats(
    var sumRms: Double = 0.0,
    var count: Int = 0,
) {
    fun avg(): Float = if (count == 0) 0f else sqrt((sumRms / count).toFloat()).toFloat()
}

class LoudnessAnalyzer {

    private val stats = mutableMapOf<SoundProfile, LoudnessStats>()
    private val lock = Any()

    fun feed(profile: SoundProfile, buffer: FloatArray, sampleRate: Int) {
        val rms = calculateRms(buffer)
        synchronized(lock) {
            val s = stats.getOrPut(profile) { LoudnessStats() }
            s.sumRms += rms
            s.count++
        }
    }

    fun getAvg(profile: SoundProfile): Float {
        synchronized(lock) {
            return stats[profile]?.avg() ?: 0f
        }
    }

    fun getBaseline(): Float {
        synchronized(lock) {
            val values = SoundProfile.values().mapNotNull { profile ->
                stats[profile]?.avg()?.takeIf { it > 0f }
            }
            if (values.isEmpty()) return 0f
            val sorted = values.sorted()
            return sorted[sorted.size / 2]
        }
    }

    fun reset() {
        synchronized(lock) {
            stats.clear()
        }
    }

    private fun calculateRms(buffer: FloatArray): Double {
        var sumSq = 0.0
        for (s in buffer) {
            sumSq += (s * s).toDouble()
        }
        return sumSq / buffer.size
    }
}
