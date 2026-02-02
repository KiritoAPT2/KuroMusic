package com.kuromusic.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/KuroMusic/KuroMusic/releases/latest")
                    .bodyAsText()
            val json = JSONObject(response)
            val versionName = json.getString("name")
            lastCheckTime = System.currentTimeMillis()
            versionName
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
