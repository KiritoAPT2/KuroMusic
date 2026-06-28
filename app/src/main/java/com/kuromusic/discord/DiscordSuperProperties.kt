package com.kuromusic.discord

import android.os.Build
import org.json.JSONObject
import java.util.Base64
import java.util.Locale
import java.util.UUID

object DiscordSuperProperties {
    private const val CLIENT_VERSION = "314.13 - Stable"
    private const val CLIENT_BUILD_NUMBER = 314013
    private const val OS_NAME = "Android"
    private const val RELEASE_CHANNEL = "stable"

    private var cachedBase64: String? = null
    private var cachedUserAgent: String? = null

    /**
     * Invalidates the cached super properties and user agent so they
     * are regenerated on the next access.
     */
    fun invalidateCache() {
        cachedBase64 = null
        cachedUserAgent = null
    }

    fun buildSuperProperties(): String {
        cachedBase64?.let { return it }

        val properties = JSONObject().apply {
            put("os", OS_NAME)
            put("browser", "Discord Android")
            put("device", Build.MODEL)
            put("system_locale", Locale.getDefault().toString())
            put("client_version", CLIENT_VERSION)
            put("release_channel", RELEASE_CHANNEL)
            put("device_vendor_id", DiscordTokenStore.getDeviceVendorId())
            put("client_uuid", DiscordTokenStore.getClientUuid())
            put("client_launch_id", UUID.randomUUID().toString())
            put("os_version", Build.VERSION.RELEASE)
            put("os_sdk_version", Build.VERSION.SDK_INT.toString())
            put("client_build_number", CLIENT_BUILD_NUMBER)
        }

        val encoded = Base64.getEncoder().encodeToString(properties.toString().toByteArray())
        cachedBase64 = encoded
        return encoded
    }

    val USER_AGENT: String
        get() {
            cachedUserAgent?.let { return it }
            val ua = buildUserAgent()
            cachedUserAgent = ua
            return ua
        }

    private fun buildUserAgent(): String {
        val osVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL
        val deviceBrand = Build.MANUFACTURER
        return "Discord-Android/$CLIENT_VERSION (${deviceBrand.lowercase()} $deviceModel; Android $osVersion; $RELEASE_CHANNEL)"
    }
}
