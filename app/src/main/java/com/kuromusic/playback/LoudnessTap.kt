package com.kuromusic.playback

object LoudnessTap {

    private var analyzer: LoudnessAnalyzer? = null

    fun attach(analyzer: LoudnessAnalyzer) {
        this.analyzer = analyzer
    }

    fun feed(profile: SoundProfile, buffer: FloatArray, sampleRate: Int) {
        analyzer?.feed(profile, buffer, sampleRate)
    }
}
