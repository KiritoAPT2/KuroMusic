package com.kuromusic.playback.cast

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope

class CastStream(val url: String, val mimeType: String)

interface CastSink {
    fun onRemotePlaybackChanged()
    fun onRemoteRouteChanged()
    fun onRemoteVolumeChanged()
}

/**
 * FOSS stub for [CastPlayback]: always inactive when Google Cast is omitted.
 */
class CastPlayback(
    @Suppress("unused") private val scope: CoroutineScope,
    @Suppress("unused") private val localPlayer: () -> ExoPlayer?,
    @Suppress("unused") private val resolve: suspend (MediaItem) -> CastStream?,
    @Suppress("unused") private val say: (Int) -> Unit,
) : RemoteSpeaker {
    override var sink: CastSink? = null
    override val active: Boolean = false
    override val playWhenReady: Boolean = false
    override val isPlaying: Boolean = false
    override val playbackState: Int = Player.STATE_IDLE
    override val positionMs: Long = 0L
    override val durationMs: Long = C.TIME_UNSET
    override val deviceVolume: Int = 0

    var resumeHereOnEnd: Boolean = false

    override fun play() = Unit
    override fun pause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun setVolume(fraction: Float) = Unit
    override fun onLocalTransition(item: MediaItem?, reason: Int) = Unit
    override fun onLocalQueueChanged() = Unit
    override fun onLocalRepeatChanged(repeatMode: Int) = Unit
    override fun onLocalStartedPlaying(player: Player) = Unit

    fun release() = Unit

    companion object {
        fun mimeTypeOf(uri: Uri): String {
            uri.getQueryParameter("mime")?.takeIf { it.isNotBlank() }?.let { return it }
            val path = uri.lastPathSegment?.lowercase().orEmpty()
            return when {
                path.endsWith(".flac") -> "audio/flac"
                path.endsWith(".mp3") -> "audio/mpeg"
                path.endsWith(".ogg") || path.endsWith(".opus") -> "audio/ogg"
                path.endsWith(".wav") -> "audio/wav"
                path.endsWith(".webm") -> "audio/webm"
                else -> "audio/mp4"
            }
        }
    }
}

