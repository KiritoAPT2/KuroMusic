package com.kuromusic.eq

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.kuromusic.eq.audio.CustomEqualizerAudioProcessor
import com.kuromusic.eq.data.ParametricEQ
import com.kuromusic.eq.data.SavedEQProfile
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerService @Inject constructor() {

    private val audioProcessors = java.util.Collections.synchronizedList(mutableListOf<CustomEqualizerAudioProcessor>())
    private var pendingProfile: SavedEQProfile? = null
    private var shouldDisable: Boolean = false

    companion object {
        private const val TAG = "EqualizerService"
    }

    @OptIn(UnstableApi::class)
    fun addAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.add(processor)
        if (shouldDisable) {
            processor.disable()
        } else if (pendingProfile != null) {
            applyProfileToProcessor(processor, pendingProfile!!)
        }
    }

    fun removeAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        audioProcessors.remove(processor)
    }

    @OptIn(UnstableApi::class)
    fun applyProfile(profile: SavedEQProfile): Result<Unit> {
        if (audioProcessors.isEmpty()) {
            pendingProfile = profile
            shouldDisable = false
            return Result.success(Unit)
        }
        pendingProfile = profile
        shouldDisable = false
        var success = true
        var lastError: Exception? = null
        audioProcessors.forEach { processor ->
            try {
                applyProfileToProcessor(processor, profile)
            } catch (e: Exception) {
                success = false; lastError = e
            }
        }
        return if (success) Result.success(Unit) else Result.failure(lastError ?: Exception("Unknown error"))
    }

    private fun applyProfileToProcessor(processor: CustomEqualizerAudioProcessor, profile: SavedEQProfile) {
        val parametricEQ = ParametricEQ(preamp = profile.preamp, bands = profile.bands)
        processor.applyProfile(parametricEQ)
    }

    @OptIn(UnstableApi::class)
    fun disable() {
        if (audioProcessors.isEmpty()) {
            shouldDisable = true
            pendingProfile = null
            return
        }
        shouldDisable = true
        pendingProfile = null
        audioProcessors.forEach { processor ->
            try { processor.disable() } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to disable equalizer: ${e.message}")
            }
        }
    }

    fun isInitialized(): Boolean = audioProcessors.isNotEmpty()

    @OptIn(UnstableApi::class)
    fun isEnabled(): Boolean = audioProcessors.any { it.isEnabled() }

    fun release() { audioProcessors.clear() }
}

data class EqualizerInfo(
    val supportsUnlimitedBands: Boolean,
    val maxBands: Int,
    val description: String
)
