package com.kuromusic.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object ArtistVideoCanvasProvider {
    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: ArtistVideoResponse?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        duration: Int? = null,
    ): CanvasArtwork? {
        val key = cacheKey("sa", song, artist, album.orEmpty())
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) {
                return entry.value?.toCanvasArtwork()
            }
            cache.remove(key)
        }

        val response = runCatching {
            client.get(BASE_URL) {
                parameter("s", song)
                parameter("a", artist)
                if (album != null) parameter("al", album)
                if (duration != null && duration > 0) parameter("d", duration)
            }
        }.getOrNull()

        val value = when (response?.status) {
            HttpStatusCode.OK -> runCatching { response.body<ArtistVideoResponse>() }.getOrNull()
            else -> null
        }

        cache[key] = CacheEntry(
            value = value,
            expiresAtMs = System.currentTimeMillis() + ttlMs,
        )

        return value?.toCanvasArtwork()
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        val normalized = parts.map { it.trim().lowercase(Locale.ROOT) }.joinToString("|")
        return "$prefix|$normalized"
    }

    private fun ArtistVideoResponse.toCanvasArtwork() = CanvasArtwork(
        name = name,
        artist = artist,
        albumId = albumId,
        static = static,
        animated = animated,
        videoUrl = videoUrl,
    )
}

@Serializable
data class ArtistVideoResponse(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
