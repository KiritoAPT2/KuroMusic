package com.kuromusic.playback

class SoundProfileStateHolder {
    @Volatile
    var profile: SoundProfile = SoundProfile.CLEAN
        private set

    @Volatile
    var state: ProfileState = ProfileState(ProfileMode.AUTO, null)
        private set

    private val autoGainMap = mutableMapOf<SoundProfile, Float>()

    fun update(newProfile: SoundProfile) {
        profile = newProfile
    }

    fun updateState(newState: ProfileState) {
        state = newState
    }

    fun setManual(profile: SoundProfile) {
        state = ProfileState(ProfileMode.MANUAL, profile)
        this.profile = profile
    }

    fun setAuto() {
        state = ProfileState(ProfileMode.AUTO, null)
    }

    fun getAutoGain(profile: SoundProfile): Float = autoGainMap[profile] ?: 0f

    fun applyAutoGain(profile: SoundProfile, gain: Float) {
        autoGainMap[profile] = gain
    }

    fun getFinalGainDb(profile: SoundProfile): Float {
        return profile.gainCompensationDb + getAutoGain(profile)
    }
}
