package com.kuromusic.playback

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kuromusic.R

enum class SoundProfile(
    @StringRes val displayNameRes: Int,
    @DrawableRes val iconRes: Int,
    val saturationPreGain: Float,
    val gainCompensationDb: Float,
) {
    CLEAN(R.string.sound_profile_clean, R.drawable.tune, 0f, 0f),
    WARM(R.string.sound_profile_warm, R.drawable.tune, 0f, 0.6f),
    BASS(R.string.sound_profile_bass, R.drawable.tune, 0f, 0.8f),
    LOFI(R.string.sound_profile_lofi, R.drawable.tune, 0f, 1.0f),
    STUDIO(R.string.sound_profile_studio, R.drawable.tune, 0f, 0.4f),
}
