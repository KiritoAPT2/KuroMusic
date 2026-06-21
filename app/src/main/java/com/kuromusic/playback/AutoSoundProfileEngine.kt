package com.kuromusic.playback

enum class ProfileMode {
    AUTO,
    MANUAL,
}

data class ProfileState(
    val mode: ProfileMode,
    val manualProfile: SoundProfile?,
)

object AutoSoundProfileEngine {

    fun resolve(device: OutputDevice, genre: Genre): SoundProfile {
        return when (device) {
            OutputDevice.SPEAKER -> when (genre) {
                Genre.LOFI -> SoundProfile.LOFI
                Genre.BASS_MUSIC -> SoundProfile.BASS
                Genre.PODCAST -> SoundProfile.WARM
                else -> SoundProfile.WARM
            }
            OutputDevice.HEADPHONES -> when (genre) {
                Genre.LOFI -> SoundProfile.LOFI
                Genre.BASS_MUSIC -> SoundProfile.BASS
                Genre.ROCK -> SoundProfile.STUDIO
                else -> SoundProfile.STUDIO
            }
            OutputDevice.BLUETOOTH -> when (genre) {
                Genre.BASS_MUSIC -> SoundProfile.BASS
                else -> SoundProfile.WARM
            }
            else -> SoundProfile.CLEAN
        }
    }
}
