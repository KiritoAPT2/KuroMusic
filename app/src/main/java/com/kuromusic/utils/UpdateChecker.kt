package com.kuromusic.utils

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kuromusic.BuildConfig

data class UpdateInfo(val version: String, val apkUrl: String)

class UpdateChecker(private val context: Context) {
    companion object {
        private const val GITHUB_OWNER = "KiritoAPT2"
        private const val GITHUB_REPO = "KuroMusic"
    }
    
    // Simple client for GitHub API
    private val client = OkHttpClient()

    suspend fun checkUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return@withContext null
                val json = JSONObject(jsonString)
                val tagName = json.optString("tag_name", "")
                val latest = tagName.removePrefix("v.").removePrefix("v")
                
                Log.i("UpdateChecker", "Current: ${BuildConfig.VERSION_NAME} | Latest: $latest")
                
                // Compare versions
                if (isNewer(latest, BuildConfig.VERSION_NAME)) {
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = json.optString("html_url") // Fallback to release page
                    
                    if (assets != null && assets.length() > 0) {
                        // Find first APK asset or just first asset
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", true)) {
                                downloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }

                    Log.i("UpdateChecker", "✅ UPDATE AVAILABLE! URL: $downloadUrl")
                    return@withContext UpdateInfo(latest, downloadUrl)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Update check failed", e)
        }
        null
    }
    
    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".")
        val localParts = local.split(".")
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
