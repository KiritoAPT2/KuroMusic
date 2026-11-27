package com.kuromusic.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * FOSS stub for [DriveAuth]: Google Play Services auth is omitted in the FOSS flavor.
 */
object DriveAuth {

    sealed interface Outcome {
        data class Granted(val accessToken: String) : Outcome
        data class NeedsConsent(val intent: PendingIntent) : Outcome
    }

    fun available(context: Context): Boolean = false

    fun isCanceled(t: Throwable): Boolean = false

    suspend fun authorize(context: Context, account: String?): Outcome =
        error("Google Drive backup is not available in the FOSS build")

    fun fromIntent(context: Context, data: Intent?): Outcome =
        error("Google Drive backup is not available in the FOSS build")

    suspend fun clearToken(context: Context, token: String) = Unit

    suspend fun revoke(context: Context, account: String) = Unit
}

