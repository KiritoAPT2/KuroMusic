package com.kuromusic.extensions

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.MimeTypes
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.db.entities.Song
import com.kuromusic.models.MediaMetadata
import com.kuromusic.models.toMediaMetadata

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

fun Song.toMediaItem() =
    MediaItem.Builder()
        .setMediaId(this@toMediaItem.id)
        .setUri(this@toMediaItem.id)
        .setCustomCacheKey(this@toMediaItem.id)
        .setTag(this@toMediaItem.toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(this@toMediaItem.title)
                .setSubtitle(this@toMediaItem.artists.joinToString { it.name })
                .setArtist(this@toMediaItem.artists.joinToString { it.name })
                .setArtworkUri(this@toMediaItem.thumbnailUrl?.toUri())
                .setAlbumTitle(this@toMediaItem.album?.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .apply {
                    if (this@toMediaItem.id.endsWith(".m4a", true)) {
                        setDiscNumber(1)
                    }
                }
                .build()
        )
        .setMimeType(if (this@toMediaItem.id.endsWith(".m4a", true)) MimeTypes.AUDIO_MP4 else null)
        .build()

fun SongItem.toMediaItem() =
    MediaItem.Builder()
        .setMediaId(this@toMediaItem.id)
        .setUri(this@toMediaItem.id)
        .setCustomCacheKey(this@toMediaItem.id)
        .setTag(this@toMediaItem.toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(this@toMediaItem.title)
                .setSubtitle(this@toMediaItem.artists.joinToString { it.name })
                .setArtist(this@toMediaItem.artists.joinToString { it.name })
                .setArtworkUri(this@toMediaItem.thumbnail.toUri())
                .setAlbumTitle(this@toMediaItem.album?.name)
                .build()
        )
        .setMimeType(if (this@toMediaItem.id.endsWith(".m4a", true)) MimeTypes.AUDIO_MP4 else null)
        .build()

fun MediaMetadata.toMediaItem() =
    MediaItem.Builder()
        .setMediaId(this@toMediaItem.id)
        .setUri(this@toMediaItem.id)
        .setCustomCacheKey(this@toMediaItem.id)
        .setTag(this@toMediaItem)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(this@toMediaItem.title)
                .setSubtitle(this@toMediaItem.artists.joinToString { it.name })
                .setArtist(this@toMediaItem.artists.joinToString { it.name })
                .setArtworkUri(this@toMediaItem.thumbnailUrl?.toUri())
                .setAlbumTitle(this@toMediaItem.album?.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .build()
        )
        .setMimeType(if (this@toMediaItem.id.endsWith(".m4a", true)) MimeTypes.AUDIO_MP4 else null)
        .build()
