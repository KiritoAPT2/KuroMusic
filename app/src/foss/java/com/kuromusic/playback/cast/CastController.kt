package com.kuromusic.playback.cast

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FOSS stub for [CastController]: Google Cast SDK is omitted in the FOSS flavor.
 */
object CastController {

    data class Device(
        val id: String,
        val name: String,
        val connecting: Boolean,
        val connected: Boolean,
    )

    data class State(
        val supported: Boolean = false,
        val devices: List<Device> = emptyList(),
        val connectedName: String? = null,
        val connecting: Boolean = false,
    ) {
        val casting: Boolean get() = connectedName != null
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun ensureStarted(context: Context) = Unit

    fun attach(target: CastPlayback) = Unit

    fun detach(target: CastPlayback) = Unit

    fun discover(active: Boolean): () -> Unit = {}

    fun connect(deviceId: String) = Unit

    fun disconnect(resumeHere: Boolean) = Unit

    fun setVolume(level: Float) = Unit
}

