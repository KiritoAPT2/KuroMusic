package com.kuromusic.discord

import org.json.JSONArray
import org.json.JSONObject

enum class ActivityType(val value: Int) {
    PLAYING(0),
    STREAMING(1),
    LISTENING(2),
    WATCHING(3),
    CUSTOM(4),
    COMPETING(5),
}

enum class PresenceStatus(val value: String) {
    ONLINE("online"),
    IDLE("idle"),
    DND("dnd"),
    INVISIBLE("invisible"),
}

object DiscordPresence {

    fun buildActivity(payload: ActivityPayload): JSONObject {
        val activity = JSONObject().apply {
            put("name", payload.name)
            put("type", payload.type)
            payload.state?.let { put("state", it) }
            payload.details?.let { put("details", it) }
            payload.url?.let { put("url", it) }

            val timestamps = JSONObject()
            payload.startMs?.let { timestamps.put("start", it) }
            payload.endMs?.let { timestamps.put("end", it) }
            if (timestamps.length() > 0) put("timestamps", timestamps)

            val assets = JSONObject()
            payload.largeImage?.let { assets.put("large_image", it) }
            payload.largeText?.let { assets.put("large_text", it) }
            payload.smallImage?.let { assets.put("small_image", it) }
            payload.smallText?.let { assets.put("small_text", it) }
            if (assets.length() > 0) put("assets", assets)

            if (payload.buttons.isNotEmpty()) {
                put("buttons", JSONArray(payload.buttons.map { it.first }))
                val metadata = JSONObject()
                metadata.put("button_urls", JSONArray(payload.buttons.map { it.second }))
                put("metadata", metadata)
            }

            put("application_id", payload.applicationId)
        }
        return activity
    }

    fun buildPresenceUpdate(
        status: PresenceStatus = PresenceStatus.ONLINE,
        afk: Boolean = false,
        since: Long? = null,
        activities: List<ActivityPayload> = emptyList(),
    ): JSONObject {
        val activitiesArray = JSONArray()
        for (activity in activities) {
            activitiesArray.put(buildActivity(activity))
        }
        return JSONObject().apply {
            put("op", 3)
            put("d", JSONObject().apply {
                put("since", since ?: JSONObject.NULL)
                put("activities", activitiesArray)
                put("status", status.value)
                put("afk", afk)
            })
        }
    }

    fun buildActivityJson(
        name: String,
        type: Int = ActivityType.LISTENING.value,
        details: String? = null,
        state: String? = null,
        url: String? = null,
        largeImage: String? = null,
        largeText: String? = null,
        smallImage: String? = null,
        smallText: String? = null,
        startMs: Long? = null,
        endMs: Long? = null,
        buttons: List<Pair<String, String>> = emptyList(),
    ): JSONObject {
        return buildActivity(ActivityPayload(
            name = name,
            type = type,
            details = details,
            state = state,
            url = url,
            largeImage = largeImage,
            largeText = largeText,
            smallImage = smallImage,
            smallText = smallText,
            startMs = startMs,
            endMs = endMs,
            buttons = buttons,
            applicationId = null,
        ))
    }
}
