package com.kuromusic.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object Updater {
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 10_000
        }
    }
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response = client.get("https://api.github.com/repos/KiritoAPT2/KuroMusic/releases/latest") {
                header("User-Agent", "KuroMusic/1.0 (Android)")
                header("Accept", "application/vnd.github+json")
            }
            val json = JSONObject(response.bodyAsText())
            val versionName = json.getString("tag_name")
            lastCheckTime = System.currentTimeMillis()
            versionName
        }

    suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
        getLatestVersionName().getOrNull()
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.removePrefix("v").split(".")
        val localParts = local.removePrefix("v").split(".")
        val length = maxOf(remoteParts.size, localParts.size)

        for (i in 0 until length) {
            val r = remoteParts.getOrNull(i)?.toIntOrNull() ?: 0
            val l = localParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
