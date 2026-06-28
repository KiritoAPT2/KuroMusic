package com.kuromusic.discord

import kotlinx.coroutines.CancellationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object DiscordExternalAssets {
    private const val TAG = "DiscordSvc"
    private const val EXTERNAL_ASSETS_API = "https://discord.com/api/v9/applications/%s/external-assets"
    private const val CACHE_MAX_SIZE = 128

    private val cache = ConcurrentHashMap<String, String>()
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000L
                connectTimeoutMillis = 5_000L
                socketTimeoutMillis = 10_000L
            }
            expectSuccess = false
        }
    }

    suspend fun resolve(imageUrl: String, appId: String, token: String): String? {
        if (imageUrl.isBlank()) return null
        if (imageUrl.startsWith("mp:")) return imageUrl

        cache[imageUrl]?.let { return it }

        return try {
            val body = JSONArray().put(imageUrl).toString()
            val response = client.post(EXTERNAL_ASSETS_API.format(appId)) {
                header("Authorization", token)
                header("User-Agent", DiscordSuperProperties.USER_AGENT)
                        header("X-Super-Properties", DiscordSuperProperties.buildSuperProperties())
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val responseBody = response.bodyAsText()
            val statusCode = response.status.value

            if (statusCode in 200..299 && responseBody.isNotBlank()) {
                val arr = JSONArray(responseBody)
                if (arr.length() > 0) {
                    val item = arr.getJSONObject(0)
                    val assetPath = item.optString("external_asset_path")
                    if (assetPath.isNotEmpty()) {
                        val result = "mp:$assetPath"
                        cache[imageUrl] = result
                        trimCache()
                        return result
                    }
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "external-assets: failed for %s", imageUrl.take(60))
            null
        }
    }

    private fun trimCache() {
        if (cache.size > CACHE_MAX_SIZE) {
            val toRemove = cache.size - CACHE_MAX_SIZE
            cache.keys.take(toRemove).forEach { cache.remove(it) }
        }
    }

    fun clearCache() {
        cache.clear()
    }

    fun close() {
        runCatching { client.close() }
    }
}
