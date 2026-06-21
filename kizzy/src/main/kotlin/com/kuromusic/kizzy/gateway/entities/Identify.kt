package com.kuromusic.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Identify(
    @SerialName("capabilities")
    val capabilities: Int,
    @SerialName("compress")
    val compress: Boolean,
    @SerialName("largeThreshold")
    val largeThreshold: Int,
    @SerialName("properties")
    val properties: Properties,
    @SerialName("token")
    val token: String,
    @SerialName("presence")
    val presence: PresencePayload? = null,
) {
    companion object {
        fun String.toIdentifyPayload() = Identify(
            capabilities = 16383,
            compress = true,
            largeThreshold = 100,
            properties = Properties(
                os = "Android",
                browser = "Discord Android",
                device = "Android",
                systemLocale = "en-US",
                browserVersion = "289.0",
                osVersion = "34",
                clientBuildNumber = 289120,
                releaseChannel = "stable",
                clientEventSource = null,
            ),
            token = this,
            presence = PresencePayload(
                activities = emptyList(),
                afk = false,
                since = 0L,
                status = "online"
            )
        )
    }
}

@Serializable
data class Properties(
    @SerialName("os")
    val os: String,
    @SerialName("browser")
    val browser: String,
    @SerialName("device")
    val device: String,
    @SerialName("system_locale")
    val systemLocale: String? = null,
    @SerialName("browser_version")
    val browserVersion: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("client_build_number")
    val clientBuildNumber: Int? = null,
    @SerialName("release_channel")
    val releaseChannel: String? = null,
    @SerialName("client_event_source")
    val clientEventSource: String? = null,
    @SerialName("referrer")
    val referrer: String? = "",
    @SerialName("referring_domain")
    val referringDomain: String? = "",
    @SerialName("referrer_current")
    val referrerCurrent: String? = "",
    @SerialName("referring_domain_current")
    val referringDomainCurrent: String? = "",
)

@Serializable
data class PresencePayload(
    @SerialName("activities")
    val activities: List<String>,
    @SerialName("afk")
    val afk: Boolean,
    @SerialName("since")
    val since: Long,
    @SerialName("status")
    val status: String,
)
