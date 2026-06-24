package com.kuromusic.sync

import com.kuromusic.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object YouTubeActionHandler {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun likeVideo(videoId: String, liked: Boolean) {
        scope.launch {
            YouTube.likeVideo(videoId, liked)
        }
    }

    fun likePlaylist(playlistId: String, liked: Boolean) {
        scope.launch {
            YouTube.likePlaylist(playlistId, liked)
        }
    }

    fun subscribeChannel(channelId: String, subscribed: Boolean) {
        scope.launch {
            YouTube.subscribeChannel(channelId, subscribed)
        }
    }

    fun subscribeArtist(artistId: String, channelId: String?, subscribed: Boolean) {
        scope.launch {
            val resolvedChannelId = channelId ?: YouTube.getChannelId(artistId)
            YouTube.subscribeChannel(resolvedChannelId, subscribed)
        }
    }

    fun registerPlayback(videoId: String?, playbackUrl: String) {
        scope.launch {
            YouTube.registerPlayback(videoId, playbackUrl)
        }
    }
}
