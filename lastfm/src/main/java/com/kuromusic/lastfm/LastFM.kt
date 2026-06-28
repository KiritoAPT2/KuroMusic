package com.kuromusic.lastfm

import com.kuromusic.lastfm.models.Authentication
import com.kuromusic.lastfm.models.LastFmError
import com.kuromusic.lastfm.models.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object LastFM {
    var sessionKey: String? = null

    private var API_KEY = ""
    private var SECRET = ""

    fun initialize(apiKey: String, secret: String) {
        API_KEY = apiKey
        SECRET = secret
    }

    fun isInitialized(): Boolean = API_KEY.isNotEmpty() && SECRET.isNotEmpty()

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest { url("https://ws.audioscrobbler.com/2.0/") }
            expectSuccess = false
        }
    }

    private fun Map<String, String>.apiSig(secret: String): String {
        val sorted = toSortedMap()
        val toHash = sorted.entries.joinToString("") { it.key + it.value } + secret
        val digest = MessageDigest.getInstance("MD5").digest(toHash.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private suspend fun lastfmRequest(
        method: String,
        apiKey: String,
        secret: String,
        sessionKey: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): String {
        val paramsForSig = mutableMapOf(
            "method" to method,
            "api_key" to apiKey
        ).apply {
            this@LastFM.sessionKey?.let { put("sk", it) }
            putAll(extra)
        }
        val apiSig = paramsForSig.apiSig(secret)
        val allParams = paramsForSig + mapOf("api_sig" to apiSig, "format" to "json")

        return client.submitForm(
            url = "https://ws.audioscrobbler.com/2.0/",
            formParameters = Parameters.build {
                allParams.forEach { (k, v) -> append(k, v) }
            }
        ).bodyAsText()
    }

    suspend fun getToken(apiKey: String, secret: String) = runCatching {
        val responseText = lastfmRequest(
            method = "auth.getToken",
            apiKey = apiKey,
            secret = secret
        )
        json.decodeFromString<TokenResponse>(responseText)
    }

    suspend fun getSession(token: String, apiKey: String, secret: String) = runCatching {
        val responseText = lastfmRequest(
            method = "auth.getSession",
            apiKey = apiKey,
            secret = secret,
            extra = mapOf("token" to token)
        )
        json.decodeFromString<Authentication>(responseText)
    }

    fun getAuthUrl(token: String, apiKey: String): String {
        return "https://www.last.fm/api/auth/?api_key=$apiKey&token=$token"
    }

    suspend fun getMobileSession(username: String, password: String, apiKey: String, secret: String) = runCatching {
        val responseText = lastfmRequest(
            method = "auth.getMobileSession",
            apiKey = apiKey,
            secret = secret,
            extra = mapOf("username" to username, "password" to password)
        )
        if (responseText.contains("\"error\"")) {
            val error = json.decodeFromString<LastFmError>(responseText)
            throw LastFmException(error.error, error.message)
        }
        json.decodeFromString<Authentication>(responseText)
    }

    class LastFmException(val code: Int, override val message: String) : Exception(message) {
        override fun toString(): String = "LastFmException(code=$code, message=$message)"
    }

    suspend fun updateNowPlaying(
        artist: String, track: String,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null,
        apiKey: String, secret: String
    ) = runCatching {
        lastfmRequest(
            method = "track.updateNowPlaying",
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
            extra = buildMap {
                put("artist", artist)
                put("track", track)
                album?.let { put("album", it) }
                trackNumber?.let { put("trackNumber", it.toString()) }
                duration?.let { put("duration", it.toString()) }
            }
        )
    }

    suspend fun scrobble(
        artist: String, track: String, timestamp: Long,
        album: String? = null, trackNumber: Int? = null, duration: Int? = null,
        apiKey: String, secret: String
    ) = runCatching {
        lastfmRequest(
            method = "track.scrobble",
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
            extra = buildMap {
                put("artist[0]", artist)
                put("track[0]", track)
                put("timestamp[0]", timestamp.toString())
                album?.let { put("album[0]", it) }
                trackNumber?.let { put("trackNumber[0]", it.toString()) }
                duration?.let { put("duration[0]", it.toString()) }
            }
        )
    }

    suspend fun setLoveStatus(
        artist: String, track: String, love: Boolean,
        apiKey: String, secret: String
    ) = runCatching {
        val method = if (love) "track.love" else "track.unlove"
        lastfmRequest(
            method = method,
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
            extra = buildMap {
                put("artist", artist)
                put("track", track)
            }
        )
    }

    const val DEFAULT_SCROBBLE_DELAY_PERCENT = 0.5f
    const val DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30
    const val DEFAULT_SCROBBLE_DELAY_SECONDS = 180
}
