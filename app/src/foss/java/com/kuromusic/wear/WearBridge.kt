package com.kuromusic.wear

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import com.kuromusic.playback.cast.CastSink
import com.kuromusic.playback.cast.RemoteSpeaker
import com.kuromusic.widget.MediaWidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FOSS stub for [WearBridge]: Wear OS Data Layer is omitted in the FOSS flavor.
 */
object WearBridge {

    private val _watchSession = MutableStateFlow(false)
    val watchSession: StateFlow<Boolean> = _watchSession.asStateFlow()

    private val _watchOutput = MutableStateFlow(WatchOutputState())
    val watchOutput: StateFlow<WatchOutputState> = _watchOutput.asStateFlow()

    fun attachStreams(factory: DataSource.Factory?) = Unit

    fun refreshWatchOutput(context: Context) = Unit

    fun selectWatchOutput() = Unit

    fun leaveWatchOutput() = Unit

    fun setWatchVolume(fraction: Float) = Unit

    fun createSpeaker(
        context: Context,
        scope: CoroutineScope,
        localPlayer: () -> ExoPlayer?,
        onChanged: () -> Unit,
    ): RemoteSpeaker = InactiveSpeaker

    fun releaseSpeaker() = Unit

    internal fun publish(
        context: Context,
        snapshot: MediaWidgetSnapshot,
        player: Player?,
        playing: Boolean?,
    ) = Unit

    fun republish(context: Context, player: Player) = Unit

    private object InactiveSpeaker : RemoteSpeaker {
        override var sink: CastSink? = null
        override val active: Boolean = false
        override val playWhenReady: Boolean = false
        override val isPlaying: Boolean = false
        override val playbackState: Int = Player.STATE_IDLE
        override val positionMs: Long = 0L
        override val durationMs: Long = C.TIME_UNSET
        override val deviceVolume: Int = 0
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(fraction: Float) = Unit
        override fun onLocalTransition(item: MediaItem?, reason: Int) = Unit
        override fun onLocalQueueChanged() = Unit
        override fun onLocalRepeatChanged(repeatMode: Int) = Unit
        override fun onLocalStartedPlaying(player: Player) = Unit
    }
}

