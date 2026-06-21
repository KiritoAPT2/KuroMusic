package com.kuromusic.playback

import kotlin.math.pow

class ProfileCalibrationSystem(
    private val analyzer: LoudnessAnalyzer,
    private val stateHolder: SoundProfileStateHolder,
) {
    private var lastCalibrationTime = 0L
    private var calibrationIntervalMs = 30_000L
    private var trackCounter = 0
    private val minTracksBeforeCalibrate = 1

    fun onTrackPlayed() {
        trackCounter++
    }

    private fun shouldCalibrate(): Boolean {
        val timeSinceLast = System.currentTimeMillis() - lastCalibrationTime
        return (timeSinceLast >= calibrationIntervalMs || trackCounter >= minTracksBeforeCalibrate)
    }

    fun calibrate() {
        if (!shouldCalibrate()) return

        val baseline = analyzer.getBaseline()
        if (baseline <= 0f) return

        for (profile in SoundProfile.values()) {
            if (profile == SoundProfile.CLEAN) continue

            val avg = analyzer.getAvg(profile)
            if (avg <= 0f) continue

            val ratio = baseline / avg
            val gainDb = 20f * kotlin.math.ln(ratio) / kotlin.math.ln(10f)
            val safeGain = gainDb.coerceIn(-3f, 3f)

            stateHolder.applyAutoGain(profile, safeGain)
        }

        lastCalibrationTime = System.currentTimeMillis()
        trackCounter = 0
    }

    fun reset() {
        analyzer.reset()
        lastCalibrationTime = 0L
        trackCounter = 0
    }
}
