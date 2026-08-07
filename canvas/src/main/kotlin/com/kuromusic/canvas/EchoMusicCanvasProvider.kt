package com.kuromusic.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

object EchoMusicCanvasProvider {
    private const val BASE_URL = "https://canvas.echomusic.fun/canvas.json"

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

    private var manifestCache: CacheEntry? = null

    private data class CacheEntry(
        val manifest: EchoMusicCanvasManifest,
        val expiresAtMs: Long,
    )

    private const val CACHE_TTL_MS = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
    ): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null

        val manifest = fetchManifest() ?: return null

        for (item in manifest.items) {
            val matchSong = song.contains(item.song, ignoreCase = true) ||
                    item.song.contains(song, ignoreCase = true)
            val matchArtist = artist.contains(item.artist, ignoreCase = true) ||
                    item.artist.contains(artist, ignoreCase = true)
            if (matchSong && matchArtist) {
                return CanvasArtwork(
                    name = item.song,
                    artist = item.artist,
                    videoUrl = item.url,
                )
            }
        }
        return null
    }

    private suspend fun fetchManifest(): EchoMusicCanvasManifest? {
        manifestCache?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) return entry.manifest
        }

        return runCatching {
            val response = client.get(BASE_URL)
            if (response.status != HttpStatusCode.OK) return@runCatching null
            val manifest = response.body<EchoMusicCanvasManifest>()
            manifestCache = CacheEntry(manifest, System.currentTimeMillis() + CACHE_TTL_MS)
            manifest
        }.getOrNull()
    }
}

@Serializable
data class EchoMusicCanvasManifest(
    val items: List<EchoMusicCanvasItem>,
)

@Serializable
data class EchoMusicCanvasItem(
    val song: String,
    val artist: String,
    val url: String,
)
