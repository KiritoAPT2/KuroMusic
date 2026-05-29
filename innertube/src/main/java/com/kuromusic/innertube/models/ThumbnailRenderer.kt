package com.kuromusic.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Upgrades a YouTube thumbnail URL to maximum resolution.
 *
 * For Google User Content CDN (YouTube Music album art):
 *   - Replaces any `=wNNN-hNNN...` suffix with `=w576-h576-l90-rj` for sharp square art.
 * For YouTube video thumbnails (ytimg.com):
 *   - Upgrades hqdefault/mqdefault/sddefault to maxresdefault for the best quality.
 */
fun String.upgradeToMaxQualityThumbnail(): String {
    // Handle YouTube Music / Google User Content CDN thumbnails (album arts / profile images)
    if (contains("lh3.googleusercontent.com") || contains("yt3.ggpht.com") || contains("yt3.googleusercontent.com")) {
        if (contains("=")) {
            val base = substringBeforeLast("=")
            val suffix = substringAfterLast("=")
            if (suffix.firstOrNull()?.let { it == 'w' || it == 's' } == true && suffix.drop(1).firstOrNull()?.isDigit() == true) {
                return if (suffix.startsWith("w")) {
                    "$base=w576-h576-l90-rj"
                } else {
                    "$base=s576-l90-rj"
                }
            }
        }
        return this
    }
    // Handle YouTube video thumbnails (ytimg.com)
    if (contains("ytimg.com")) {
        val cleanUrl = substringBefore("?")
        val filename = cleanUrl.substringAfterLast("/")
        if (filename == "default.jpg" || filename == "mqdefault.jpg" || filename == "sddefault.jpg" || filename == "maxresdefault.jpg" || filename == "hqdefault.jpg") {
            return cleanUrl.substringBeforeLast("/") + "/hqdefault.jpg"
        }
        return cleanUrl
    }
    return this
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    fun getThumbnailUrl() = musicThumbnailRenderer?.getThumbnailUrl()
        ?: croppedSquareThumbnailRenderer?.getThumbnailUrl()
        ?: musicAnimatedThumbnailRenderer?.animatedThumbnail?.thumbnails?.lastOrNull()?.url?.upgradeToMaxQualityThumbnail()
        ?: musicAnimatedThumbnailRenderer?.backupRenderer?.getThumbnailUrl()

    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl() = thumbnail.thumbnails.lastOrNull()?.url?.upgradeToMaxQualityThumbnail()
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
