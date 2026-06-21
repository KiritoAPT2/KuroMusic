package com.kuromusic.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.kuromusic.innertube.models.response.PlayerResponse
import com.kuromusic.innertube.pages.NewPipeUtils
import com.kuromusic.constants.AudioQuality
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.YouTubeClient
import com.kuromusic.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.kuromusic.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.kuromusic.innertube.models.YouTubeClient.Companion.IOS
import com.kuromusic.innertube.models.YouTubeClient.Companion.MOBILE
import com.kuromusic.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.kuromusic.innertube.models.YouTubeClient.Companion.WEB
import com.kuromusic.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.kuromusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import com.kuromusic.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Response
import okhttp3.Cache
import android.content.Context
import java.net.URLDecoder
import java.io.File
import java.net.URI
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers


object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val MUSIC_CLIENT_HEADERS = mapOf(
        "User-Agent" to "com.google.android.apps.youtube.music/7.01.52 (Linux; U; Android 15; Pixel 9 Pro)",
        "X-YouTube-Client-Name" to "21",
        "X-YouTube-Client-Version" to "7.01.52",
        "X-YouTube-API-Key" to BuildConfig.INNER_TUBE_API_KEY,
        "Accept-Language" to "es-419,es;q=0.9,en;q=0.8"
    )

    private var cacheDir: File? = null
    private var httpClient: OkHttpClient? = null

    fun initialize(context: Context) {
        if (httpClient != null) return
        cacheDir = context.cacheDir
        httpClient = createMusicClient()
    }

    private fun createMusicClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        
        // Cache 50MB
        cacheDir?.let {
            val cacheSize = 50L * 1024 * 1024 // 50MB
            val cache = Cache(File(it, "http_cache"), cacheSize)
            builder.cache(cache)
        }

        return builder
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val cookiesStr = YouTube.cookie ?: BuildConfig.YOUTUBE_SESSION_COOKIES
                    return if (cookiesStr.isNotBlank()) {
                         cookiesStr.split("; ").mapNotNull { 
                             Cookie.parse(url, it)
                         }
                    } else emptyList()
                }
            })
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply { MUSIC_CLIENT_HEADERS.forEach { addHeader(it.key, it.value) } }
                    // Force cache usage for 1 hour
                    .header("Cache-Control", "public, max-age=3600")
                    .build()
                chain.proceed(request)
            }
            .proxy(YouTube.proxy)
            .build()
    }

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.metrolist.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_NO_AUTH
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_MUSIC,
        WEB_REMIX,
        MOBILE,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    // Cache to prevent re-fetching PlayerResponse (Speed up by ~1s)
    private val playerCache = mutableMapOf<String, Result<PlayerResponse>>()
    private val cacheTime = mutableMapOf<String, Long>()
    private const val MAX_CACHE_ENTRIES = 200
    private const val CACHE_TTL_MS = 60 * 60 * 1000L

    fun trimCache() {
        val now = System.currentTimeMillis()
        val staleKeys = cacheTime.filter { (_, time) -> now - time > CACHE_TTL_MS }.keys
        staleKeys.forEach { key ->
            playerCache.remove(key)
            cacheTime.remove(key)
        }
        if (playerCache.size > MAX_CACHE_ENTRIES) {
            val extra = playerCache.size - MAX_CACHE_ENTRIES
            val toRemove = cacheTime.entries.sortedBy { it.value }.take(extra).map { it.key }
            toRemove.forEach { key ->
                playerCache.remove(key)
                cacheTime.remove(key)
            }
        }
    }

    private suspend fun getCachedPlayerResponse(
        videoId: String, 
        playlistId: String?, 
        client: YouTubeClient, 
        signatureTimestamp: Int?
    ): Result<PlayerResponse> {
        val now = System.currentTimeMillis()
        val key = "$videoId-${client.clientName}"
        
        if (playerCache.containsKey(key)) {
            val timestamp = cacheTime[key] ?: 0L
            if (now - timestamp < 60 * 60 * 1000) { // 1 hour cache
                Timber.tag(logTag).d("⚡ Using cached response for $key")
                return playerCache[key]!!
            }
        }
        
        val response = YouTube.player(videoId, playlistId, client, signatureTimestamp)
        playerCache[key] = response
        cacheTime[key] = now
        return response
    }
    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure client is initialized (fallback if not called)
            if (httpClient == null) httpClient = createMusicClient() 

        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            getCachedPlayerResponse(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // Safety: Limit retries to 3 (Main + 3 fallbacks = 4 attempts total)
            if (clientIndex >= 2) {
                 Timber.tag(logTag).w("Retry limit reached (Main + 3 fallbacks). Aborting to prevent freeze.")
                 break
            }
            
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    getCachedPlayerResponse(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                Timber.tag(logTag).d("streamingData: ${streamPlayerResponse?.streamingData}")
                Timber.tag(logTag).d("Adaptive formats: ${streamPlayerResponse?.streamingData?.adaptiveFormats?.size}")

                Timber.tag(logTag).d("✅ Player response received. hasStreamingData: ${streamPlayerResponse?.streamingData != null}")
                
                val adaptiveFormatsSize = streamPlayerResponse?.streamingData?.adaptiveFormats?.size ?: 0
                val formatsSize = streamPlayerResponse?.streamingData?.formats?.size ?: 0
                Timber.tag(logTag).d("Formats: $formatsSize, AdaptiveFormats: $adaptiveFormatsSize")

                if (streamPlayerResponse?.streamingData == null) {
                    Timber.tag(logTag).e("❌ NO STREAMING DATA - YouTube blocked extraction or obfuscation changed.")
                    Timber.tag(logTag).e("Response status: ${streamPlayerResponse?.playabilityStatus?.status}")
                }

                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).e("❌ No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}, itag: ${format.itag}")

                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) {
                    Timber.tag(logTag).e("❌ Stream URL not found (decryption failed?) for format itag: ${format.itag}")
                    
                    // FALLBACK: Piped API
                    streamUrl = getPipedStreamUrl(videoId)
                    if (streamUrl != null) {
                         Timber.tag(logTag).d("🎉 Used Piped fallback URL")
                    } else {
                         Timber.tag(logTag).e("❌ Piped fallback also failed")
                         continue
                    }
                } else {
                     Timber.tag(logTag).d("✅ Stream URL obtained successfully")
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    Timber.tag(logTag).d("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    break
                }
                
                // ⚡ OPTIMIZATION: Skip validation for everyone to speed up playback
                Timber.tag(logTag).i("⚡ Skipping validation for speed (Instant Play)")
                break

                /* 
                // Removed to optimize speed
                if (validateStatus(streamUrl)) {
                    // working stream found
                    Timber.tag(logTag).d("Stream validated successfully with client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                }
                */
            } else {
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }
}
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio } ?: emptyList()

        val safeFormat = when (audioQuality) {
            AudioQuality.HIGH -> adaptiveFormats.find { it.itag == 141 }
                ?: adaptiveFormats.find { it.itag == 251 }
                ?: adaptiveFormats.find { it.itag == 140 }
            else -> adaptiveFormats.find { it.itag == 251 }
                ?: adaptiveFormats.find { it.itag == 140 }
        } ?: adaptiveFormats.maxByOrNull {
            it.bitrate * when (audioQuality) {
                AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                AudioQuality.HIGH -> 1
                AudioQuality.LOW -> -1
            } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
        }

        if (safeFormat != null) {
            Timber.tag(logTag).d("Selected format: ${safeFormat.mimeType}, bitrate: ${safeFormat.bitrate}, itag: ${safeFormat.itag}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return safeFormat
    }
    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = Request.Builder()
                .head()
                .url(url)
            val response = httpClient!!.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Signature timestamp bypass for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure { Timber.tag(logTag).w(it, "Failed to get signature timestamp") }
            .getOrNull()
    }
    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    /**
     * Tries to find the Direct URL from the format.
     * Bypasses NewPipe logic if a direct URL is available to avoid extraction errors.
     * Falls back to naive signature parsing if direct URL is missing but cipher is present.
     */
    private suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        // 1️⃣ DIRECT URL
        val url = format.url
        if (!url.isNullOrEmpty()) {
            Timber.tag(logTag).i("✅ DIRECT URL found for itag ${format.itag}")
            return url
        }

        // 2️⃣ SignatureCipher fallback (Naive parse)
        val sigCipher = format.signatureCipher
        if (!sigCipher.isNullOrEmpty()) {
            Timber.tag(logTag).d("Attempting manual signature parsing for itag ${format.itag}")
            return parseSignatureCipher(sigCipher)
        }

        // 3️⃣ NewPipe Fallback (Last resort for extraction if above fails, though usually Format has one of the above)
        Timber.tag(logTag).d("No direct URL/Cipher, trying NewPipeUtils extraction...")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained via NewPipe") }
            .onFailure {
                Timber.tag(logTag).e(it, "NewPipe failed to get stream URL")
            }
            .getOrNull()
    }

    private fun parseSignatureCipher(sigCipher: String): String? {
        return try {
            val params = sigCipher.split('&').associate {
                val parts = it.split('=', limit = 2)
                if (parts.size == 2) {
                    parts[0] to URLDecoder.decode(parts[1], "UTF-8")
                } else {
                    parts[0] to ""
                }
            }
            params["url"]?.let { "$it&${params["s"]?.let { s -> "sig=$s" } ?: ""}" }
        } catch (e: Exception) {
             Timber.tag(logTag).e(e, "Failed to parse signature cipher")
             null
        }
    }

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks/streams/",
        "https://pipedapi.garudalinux.org/streams/",
        "https://api.pipepipe.pw/streams/",
        "https://pipedapi.librex.me/streams/",
        "https://vid.puffyan.us/api/v1/streams/"  // Backup
    )

    private suspend fun getPipedStreamUrl(videoId: String): String? {
        for (baseUrl in PIPED_INSTANCES) {
            val url = "$baseUrl$videoId"
            try {
                Timber.tag("PipedDebug").d("🔍 $url")

                val client = httpClient!!.newBuilder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                Timber.tag("PipedDebug").d("$baseUrl → ${response.code}")

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body == null) continue

                    // Check for HTML response (invalid)
                    if (body.trim().startsWith("<!DOCTYPE") || body.contains("<html", true)) {
                        Timber.tag("PipedDebug").w("$baseUrl → HTML instead of JSON")
                        continue
                    }

                    val json = JSONObject(body)
                    val audioStreams = json.optJSONArray("audioStreams")
                    if (audioStreams != null && audioStreams.length() > 0) {
                        val stream = audioStreams.getJSONObject(0)
                        val quality = stream.optString("quality", "unknown")
                        Timber.tag(logTag).i("✅ Piped $quality from $baseUrl")
                        return stream.getString("url")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("PipedDebug").w("$baseUrl → ${e.message}")
            }
            delay(150) // Rate limit
        }
        return null
    }
    private fun refreshCookiesIfNeeded(response: Response): Boolean {
        if (response.code == 401 || response.code == 403) {
            Timber.tag(logTag).w("Cookies refresh needed (401/403)")
            return true
        }
        return false
    }

    fun cleanStreamUrl(url: String): String {
        return url
            .replace("\\s+".toRegex(), "")  // Quita espacios
            .replace("%0A", "")             // Quita newlines
            .replace("\n", "").replace("\r", "") // CR/LF
            .replace("\t", "")              // Tabs
            .trim()
    }

    fun getValidStreamUrl(format: PlayerResponse.StreamingData.Format): String {
        val url = format.url
        Timber.tag(logTag).d("Raw URL: $url") // DEBUG

        val validUrl = if (url.isValidUrl()) {
            url!!
        } else {
            // FIX: reconstruye con signatureCipher si existe
            format.signatureCipher?.let { parseSignatureCipher(it) } ?: url ?: ""
        }
        return cleanStreamUrl(validUrl)
    }

    private fun String?.isValidUrl(): Boolean {
        if (this == null) return false
        return this.contains("googlevideo.com/videoplayback?") &&
                this.length > 100 && // sanity check
                !this.contains("undefined") &&
                URI.create(this).isAbsolute
    }
}
