package com.kuromusic.playback

data class AudioState(
    val loudnessDb: Float?,
    val gainDb: Float,
    val targetLufs: Float
)

class AudioStateHolder {
    @Volatile
    private var state = AudioState(
        loudnessDb = null,
        gainDb = 0f,
        targetLufs = -14f
    )

    fun update(newState: AudioState) {
        state = newState
    }

    fun get(): AudioState = state
}
