package com.kuromusic.discord

object DiscordDefaults {
    const val YOUTUBE_WATCH_URL = "https://music.youtube.com/watch?v="
    const val BUTTON1_LABEL = "Listen on YouTube Music"
    const val BUTTON1_URL_TEMPLATE = "https://music.youtube.com/watch?v={song.id}"
    const val BUTTON2_LABEL = "Visit KuroMusic"
    const val BUTTON2_URL = "https://github.com/KiritoAPT2/KuroMusic"
    const val STATE_TEMPLATE = "{artist.name}"
    const val DETAILS_TEMPLATE = "{song.name}"
    const val ACTIVITY_TYPE = "2"
    const val DISCORD_OAUTH_AUTHORIZE = "https://discord.com/api/oauth2/authorize"
    const val DISCORD_OAUTH_TOKEN = "https://discord.com/api/oauth2/token"
    const val DISCORD_SCOPES = "openid sdk.social_layer_presence"
}
