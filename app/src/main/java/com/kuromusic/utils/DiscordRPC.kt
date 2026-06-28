package com.kuromusic.utils

import com.kuromusic.db.entities.Song
import com.kuromusic.discord.DiscordRpcManager

class DiscordRPC {
    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
    ) {
        val currentTime = System.currentTimeMillis()
        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime
        val remainingDuration = song.song.duration * 1000L - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val songTitleWithRate = if (playbackSpeed != 1.0f) {
            "${song.song.title} [${String.format("%.2fx", playbackSpeed)}]"
        } else {
            song.song.title
        }

        DiscordRpcManager.setActivity(
            name = "KuroMusic",
            type = 2,
            details = songTitleWithRate,
            state = song.artists.joinToString { it.name },
            largeImage = song.song.thumbnailUrl,
            largeText = song.album?.title,
            smallImage = song.artists.firstOrNull()?.thumbnailUrl,
            smallText = song.artists.firstOrNull()?.name,
            startMs = calculatedStartTime,
            endMs = currentTime + adjustedRemainingDuration,
            buttons = listOf(
                "Listen on KuroMusic" to "https://music.youtube.com/watch?v=${song.song.id}",
                "Visit KuroMusic" to "https://github.com/KiritoAPT2/KuroMusic"
            ),
            songId = song.song.id,
            isPlaying = true,
        )
    }

    fun stopActivity() {
        DiscordRpcManager.clear()
    }

    fun isRpcRunning(): Boolean = DiscordRpcManager.isReady()
}
