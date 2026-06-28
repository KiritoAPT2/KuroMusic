package com.kuromusic.discord

sealed class ReconnectAction {
    data object DISCONNECT : ReconnectAction()
    data object RESUME : ReconnectAction()
    data object RECONNECT : ReconnectAction()
    data object REFRESH_TOKEN : ReconnectAction()
}

object DiscordReconnectDecider {
    fun decide(code: Int, reason: String): ReconnectAction {
        return when {
            // User-initiated disconnect (app closing)
            code == 1000 && reason == "destroy" -> ReconnectAction.DISCONNECT
            code == 1000 && reason == "user disconnect" -> ReconnectAction.DISCONNECT

            // Token expired — refresh and re-identify
            code == 4004 -> ReconnectAction.REFRESH_TOKEN

            // Invalid seq — resume not possible
            code == 4007 -> ReconnectAction.RECONNECT
            code == 4009 -> ReconnectAction.RECONNECT

            // Session no longer valid
            code == 4000 -> ReconnectAction.RESUME

            // Rate limited or unknown — reconnect with backoff
            code in 4001..4003 -> ReconnectAction.RESUME
            code == 4005 -> ReconnectAction.RESUME
            code == 4014 -> ReconnectAction.RESUME

            // Transport failure (code -1 from onFailure) — resume
            code == -1 -> ReconnectAction.RESUME

            // Default: treat as resumable
            else -> ReconnectAction.RESUME
        }
    }
}
