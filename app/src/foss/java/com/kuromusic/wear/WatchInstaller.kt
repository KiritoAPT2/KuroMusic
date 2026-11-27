package com.kuromusic.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FOSS stub for [WatchInstaller]: Wear OS Data Layer is omitted in the FOSS flavor.
 */
object WatchInstaller {

    private val _watches = MutableStateFlow<List<WatchEntry>?>(emptyList())
    val watches: StateFlow<List<WatchEntry>?> = _watches.asStateFlow()

    private val _update = MutableStateFlow<WatchUpdateState>(WatchUpdateState.Idle)
    val update: StateFlow<WatchUpdateState> = _update.asStateFlow()

    private val _pairing = MutableStateFlow<WatchPairingState>(WatchPairingState.Idle)
    val pairing: StateFlow<WatchPairingState> = _pairing.asStateFlow()

    fun available(context: Context): Boolean = false

    fun refresh(context: Context) = Unit

    fun sendUpdate(context: Context, nodeId: String) = Unit

    fun startPairing(context: Context) = Unit

    fun stopPairing() = Unit

    fun pairAndInstall(context: Context, nodeId: String, code: String) = Unit
}

