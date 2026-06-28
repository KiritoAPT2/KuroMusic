package com.kuromusic.discord

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.kuromusic.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Thrown when the user cancels the Discord OAuth2 authorization flow.
 */
class UserCancelled(
    message: String = "User cancelled the Discord authorization flow",
) : Exception(message)

/**
 * Thrown when a network request to Discord fails.
 */
class NetworkFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thrown when Discord returns an invalid_grant error during token exchange or refresh.
 */
class InvalidGrant(
    message: String,
) : Exception(message)

/**
 * Thrown when the OAuth state parameter from the callback does not match the generated state.
 */
class StateMismatch(
    message: String = "OAuth state parameter mismatch detected",
) : Exception(message)

/**
 * Thrown when no browser is available to launch the Discord authorization page.
 */
class NoBrowser(
    message: String = "No browser available to open the Discord authorization URL",
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * PKCE code verifier and challenge pair generated for the OAuth2 flow.
 */
data class PkcePair(
    val verifier: String,
    val challenge: String,
)

/**
 * Result extracted from the Discord OAuth2 redirect callback.
 */
data class AuthCodeResult(
    val authCode: String,
    val state: String,
)

/**
 * Successful result of a Discord OAuth2 token exchange or refresh.
 */
data class DiscordAuthResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshToken: String,
    val scope: String,
)

@Serializable
private data class DiscordTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("scope") val scope: String,
)

@Serializable
private data class DiscordErrorResponse(
    val error: String,
    @SerialName("error_description") val errorDescription: String? = null,
)

/**
 * Manages the Discord OAuth2 PKCE authorization flow for KuroMusic.
 *
 * Uses CustomTabs to open the Discord authorization page and relies on
 * [DiscordOAuthActivity] to receive the redirect callback via the custom scheme
 * [REDIRECT_URI]. The authorization code is then exchanged for tokens via
 * Discord's OAuth2 token endpoint.
 *
 * @property httpClient Ktor HTTP client with CIO engine used for token exchange.
 */
class DiscordAuth {

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 15_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val secureRandom = SecureRandom()

    /**
     * Generates a PKCE code verifier and its corresponding SHA-256 challenge.
     *
     * The verifier is 64 random bytes Base64URL-encoded (no padding).
     * The challenge is the verifier hashed with SHA-256 and Base64URL-encoded (no padding).
     */
    fun generatePkcePair(): PkcePair {
        val codeVerifierBytes = ByteArray(64)
        secureRandom.nextBytes(codeVerifierBytes)
        val codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes)

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.UTF_8))
        val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

        Timber.tag(TAG).d("Generated PKCE pair (verifier=${codeVerifier.length} bytes)")
        return PkcePair(verifier = codeVerifier, challenge = codeChallenge)
    }

    /**
     * Generates a cryptographically random state string for CSRF protection.
     */
    fun generateState(): String {
        val stateBytes = ByteArray(32)
        secureRandom.nextBytes(stateBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
    }

    /**
     * Initiates the Discord OAuth2 PKCE authorization flow.
     *
     * 1. Cancels any existing pending authorization.
     * 2. Generates a PKCE pair and a random state value.
     * 3. Opens the Discord authorization page via CustomTabs (with browser fallback).
     * 4. Suspends and waits for [DiscordOAuthActivity] to deliver the callback result.
     * 5. Validates the state parameter from the callback.
     * 6. Exchanges the authorization code for tokens.
     *
     * @param activity The Android [Activity] used to launch the CustomTabs intent.
     * @return A [DiscordAuthResult] containing the access and refresh tokens.
     * @throws UserCancelled If the user cancels the authorization flow.
     * @throws StateMismatch If the OAuth state parameter does not match.
     * @throws InvalidGrant If the authorization code is invalid or expired.
     * @throws NetworkFailure If a network error occurs during token exchange.
     * @throws NoBrowser If no browser is available to open the authorization URL.
     */
    suspend fun authorize(activity: Activity): DiscordAuthResult {
        Timber.tag(TAG).d("Starting Discord OAuth2 authorization flow")

        // Clear any stale pending authorization
        cancel()

        val pkcePair = generatePkcePair()
        val state = generateState()
        val deferred = CompletableDeferred<AuthCodeResult>()
        pendingAuth = deferred

        val authorizeUri = buildAuthorizationUri(pkcePair.challenge, state)
        Timber.tag(TAG).d("Authorization URI built, launching CustomTabs")

        try {
            launchCustomTabs(activity, authorizeUri)
        } catch (e: Exception) {
            pendingAuth = null
            Timber.tag(TAG).e(e, "Failed to launch browser for Discord authorization")
            throw NoBrowser(cause = e)
        }

        val authCodeResult: AuthCodeResult
        try {
            authCodeResult = deferred.await()
        } catch (e: Exception) {
            pendingAuth = null
            when (e) {
                is UserCancelled -> throw e
                is StateMismatch -> throw e
                else -> throw NetworkFailure("Discord authorization was interrupted", e)
            }
        }
        pendingAuth = null

        // Validate state to prevent CSRF attacks
        if (authCodeResult.state != state) {
            Timber.tag(TAG).w("State mismatch: expected=[$state], received=[${authCodeResult.state}]")
            throw StateMismatch()
        }

        Timber.tag(TAG).d("Authorization code received, exchanging for tokens")
        return exchangeCodeForTokens(authCodeResult.authCode, pkcePair.verifier)
    }

    /**
     * Refreshes an expired Discord access token using the refresh token.
     *
     * @param refreshToken The refresh token obtained from a previous successful authorization.
     * @return A new [DiscordAuthResult] containing fresh access and (possibly new) refresh tokens.
     * @throws InvalidGrant If the refresh token is invalid or expired.
     * @throws NetworkFailure If a network error occurs.
     */
    suspend fun refresh(refreshToken: String): DiscordAuthResult {
        Timber.tag(TAG).d("Refreshing Discord access token")

        val response = httpClient.submitForm(
            url = DISCORD_OAUTH_TOKEN,
            formParameters = Parameters.build {
                append("client_id", BuildConfig.DISCORD_APP_ID.toString())
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            },
        )

        val body = response.bodyAsText()
        Timber.tag(TAG).d("Token refresh response status: ${response.status.value}")

        return handleTokenResponse(body)
    }

    /**
     * Cancels any pending Discord authorization flow by completing the
     * pending deferred with a [UserCancelled] exception.
     *
     * Safe to call even when no authorization is in progress.
     */
    fun cancel() {
        pendingAuth?.let { deferred ->
            if (!deferred.isCompleted) {
                Timber.tag(TAG).d("Cancelling pending Discord authorization")
                deferred.completeExceptionally(UserCancelled())
            }
        }
        pendingAuth = null
    }

    /**
     * Releases resources held by the underlying HTTP client.
     *
     * After calling this method, the [DiscordAuth] instance should not be reused.
     * Create a new instance for subsequent authorization flows.
     */
    fun close() {
        Timber.tag(TAG).d("Closing DiscordAuth HTTP client")
        httpClient.close()
    }

    /**
     * Builds the Discord OAuth2 authorization URI with all required query parameters.
     */
    private fun buildAuthorizationUri(codeChallenge: String, state: String): Uri {
        return Uri.parse(DISCORD_OAUTH_AUTHORIZE).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.DISCORD_APP_ID.toString())
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .build()
    }

    /**
     * Launches the Discord authorization page using Chrome CustomTabs.
     *
     * Falls back to a regular browser intent if CustomTabs is not available
     * on the device.
     *
     * @throws Exception if no browser activity can handle the URI.
     */
    private fun launchCustomTabs(activity: Activity, uri: Uri) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .setInstantAppsEnabled(false)
            .build()

        try {
            customTabsIntent.launchUrl(activity, uri)
        } catch (_: Exception) {
            // Fallback to a standard browser intent
            Timber.tag(TAG).d("CustomTabs not available, falling back to browser intent")
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (browserIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(browserIntent)
            } else {
                throw NoBrowser("No browser or CustomTabs available on this device")
            }
        }
    }

    /**
     * Exchanges the authorization code for an access token and refresh token
     * by POSTing to the Discord OAuth2 token endpoint.
     */
    private suspend fun exchangeCodeForTokens(code: String, codeVerifier: String): DiscordAuthResult {
        Timber.tag(TAG).d("Exchanging authorization code for tokens")

        val response = httpClient.submitForm(
            url = DISCORD_OAUTH_TOKEN,
            formParameters = Parameters.build {
                append("client_id", BuildConfig.DISCORD_APP_ID.toString())
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", REDIRECT_URI)
                append("code_verifier", codeVerifier)
            },
        )

        val body = response.bodyAsText()
        Timber.tag(TAG).d("Token exchange response status: ${response.status.value}")

        return handleTokenResponse(body)
    }

    /**
     * Parses the Discord OAuth2 token endpoint response.
     *
     * On success, returns a [DiscordAuthResult].
     * On failure, throws an appropriate exception based on the error type.
     */
    private fun handleTokenResponse(body: String): DiscordAuthResult {
        // Try to parse as a successful response first
        val tokenResponse = try {
            json.decodeFromString<DiscordTokenResponse>(body)
        } catch (_: Exception) {
            null
        }

        if (tokenResponse != null) {
            Timber.tag(TAG).d("Token exchange/refresh succeeded")
            return DiscordAuthResult(
                accessToken = tokenResponse.accessToken,
                tokenType = tokenResponse.tokenType,
                expiresIn = tokenResponse.expiresIn,
                refreshToken = tokenResponse.refreshToken,
                scope = tokenResponse.scope,
            )
        }

        // Parse error response
        val errorResponse = try {
            json.decodeFromString<DiscordErrorResponse>(body)
        } catch (_: Exception) {
            null
        }

        val error = errorResponse?.error ?: "unknown"
        val description = errorResponse?.errorDescription ?: "No error description provided"
        Timber.tag(TAG).w("Discord returned error: $error - $description")

        throw when (error) {
            "invalid_grant" -> InvalidGrant(description)
            else -> NetworkFailure("Discord token request failed: $error - $description")
        }
    }

    companion object {
        private const val TAG = "DiscordSvc"

        /**
         * Discord OAuth2 authorization endpoint.
         */
        private const val DISCORD_OAUTH_AUTHORIZE = "https://discord.com/oauth2/authorize"

        /**
         * Discord OAuth2 token endpoint (API v10).
         */
        private const val DISCORD_OAUTH_TOKEN = "https://discord.com/api/v10/oauth2/token"

        /**
         * Custom scheme redirect URI handled by [DiscordOAuthActivity].
         */
        private const val REDIRECT_URI = "kuromusicdiscord://oauth2/callback"

        /**
         * Required OAuth2 scopes for Discord Rich Presence.
         */
        private const val SCOPES = "identify rpc.activities.write"

        /**
         * Holds the deferred result for the currently pending authorization flow.
         * Written by [authorize] and resolved by [DiscordOAuthActivity] via
         * [completeAuth] or [failAuth].
         */
        private var pendingAuth: CompletableDeferred<AuthCodeResult>? = null

        /**
         * Called by [DiscordOAuthActivity] when the OAuth2 redirect callback
         * is received successfully with an authorization code.
         */
        fun completeAuth(result: AuthCodeResult) {
            Timber.tag(TAG).d("Authorization callback received with auth code")
            pendingAuth?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.complete(result)
                }
            }
        }

        /**
         * Called by [DiscordOAuthActivity] when the OAuth2 redirect callback
         * indicates an error or an exception occurs.
         */
        fun failAuth(exception: Throwable) {
            Timber.tag(TAG).w("Authorization callback failed: ${exception.message}")
            pendingAuth?.let { deferred ->
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(exception)
                }
            }
        }
    }
}
