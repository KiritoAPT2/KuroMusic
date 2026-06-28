package com.kuromusic.discord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Invisible Activity that receives the Discord OAuth2 redirect callback
 * via the custom scheme "kuromusicdiscord://oauth2/callback".
 *
 * The activity has [android.R.style.Theme_NoDisplay] so it never renders
 * a UI. It extracts the authorization code and state from the incoming
 * URI, then completes (or exceptionally completes) the
 * [CompletableDeferred] in the companion object so that [DiscordAuth]
 * can continue the authorization flow.
 *
 * Registered in AndroidManifest.xml with:
 * ```xml
 * <activity
 *     android:name=".discord.DiscordOAuthActivity"
 *     android:exported="true"
 *     android:launchMode="singleTask"
 *     android:theme="@android:style/Theme.NoDisplay">
 *     <intent-filter>
 *         <action android:name="android.intent.action.VIEW" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *         <category android:name="android.intent.category.BROWSABLE" />
 *         <data
 *             android:scheme="kuromusicdiscord"
 *             android:host="oauth2"
 *             android:pathPrefix="/callback" />
 *     </intent-filter>
 * </activity>
 * ```
 */
class DiscordOAuthActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.tag(TAG).d("OAuth callback received")

        try {
            val uri = intent?.data
            if (uri == null) {
                Timber.tag(TAG).w("OAuth callback with null URI")
                failAuth(IllegalStateException("Redirect callback received with no URI"))
                return
            }

            Timber.tag(TAG).d("Callback URI: $uri")

            // Check for error parameter
            val error = uri.getQueryParameter("error")
            if (error != null) {
                val errorDescription = uri.getQueryParameter("error_description")
                val message = if (errorDescription != null) {
                    "Discord OAuth error: $error - $errorDescription"
                } else {
                    "Discord OAuth error: $error"
                }
                Timber.tag(TAG).w(message)
                failAuth(RuntimeException(message))
                return
            }

            // Extract authorization code
            val code = uri.getQueryParameter("code")
            if (code.isNullOrBlank()) {
                Timber.tag(TAG).w("OAuth callback missing 'code' parameter")
                failAuth(IllegalStateException("Redirect callback missing 'code' parameter"))
                return
            }

            // Extract state parameter
            val state = uri.getQueryParameter("state")
            if (state.isNullOrBlank()) {
                Timber.tag(TAG).w("OAuth callback missing 'state' parameter")
                failAuth(IllegalStateException("Redirect callback missing 'state' parameter"))
                return
            }

            Timber.tag(TAG).d("Authorization code and state received successfully")
            completeAuth(AuthCodeResult(authCode = code, state = state))

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Unexpected error processing OAuth callback")
            failAuth(e)
        } finally {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the intent so onCreate sees the latest data
        // (relevant when launchMode is singleTask)
        setIntent(intent)
    }

    companion object {
        private const val TAG = "DiscordSvc"

        /**
         * The [CompletableDeferred] that [DiscordAuth.authorize] awaits on.
         * Set by [newDeferred] before the authorization URI is launched.
         */
        private var pendingDeferred: CompletableDeferred<AuthCodeResult>? = null

        /**
         * Creates a new deferred and replaces any previous pending one.
         *
         * @return The new [CompletableDeferred] that will be completed by this activity.
         */
        fun newDeferred(): CompletableDeferred<AuthCodeResult> {
            val deferred = CompletableDeferred<AuthCodeResult>()
            pendingDeferred = deferred
            return deferred
        }

        /**
         * Awaits the completion of the current deferred with a timeout.
         *
         * @param timeoutMs Maximum time to wait in milliseconds (default 120_000 = 2 minutes).
         * @return The [AuthCodeResult] if completed successfully.
         * @throws Exception if the deferred completes exceptionally or the timeout expires.
         */
        fun awaitCode(timeoutMs: Long = 120_000L): AuthCodeResult {
            val deferred = pendingDeferred
                ?: throw IllegalStateException("No pending authorization deferred. Call newDeferred() first.")

            return runBlocking {
                val result = deferred.await()
                // Note: runBlocking with timeout could use withTimeout;
                // the deferred itself is cancelled externally by cancelPending().
                result
            }
        }

        /**
         * Cancels any pending authorization by exceptionally completing the
         * deferred with a [UserCancelled] exception.
         */
        fun cancelPending() {
            pendingDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    Timber.tag(TAG).d("Cancelling pending OAuth authorization")
                    deferred.completeExceptionally(
                        UserCancelled("Discord authorization was cancelled by the user")
                    )
                }
            }
            pendingDeferred = null
        }

        /**
         * Completes the pending deferred with a successful [AuthCodeResult].
         *
         * Also delegates to [DiscordAuth.completeAuth] for backward compatibility
         * with the existing authorization flow.
         *
         * @param result The authorization code and state extracted from the callback URI.
         */
        private fun completeAuth(result: AuthCodeResult) {
            // Complete our own deferred (new mechanism)
            pendingDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    Timber.tag(TAG).d("Completing deferred with auth code result")
                    deferred.complete(result)
                }
            }
            pendingDeferred = null

            // Also notify the legacy DiscordAuth companion (backward compatibility)
            DiscordAuth.completeAuth(result)
        }

        /**
         * Exceptionally completes the pending deferred with the given error.
         *
         * Also delegates to [DiscordAuth.failAuth] for backward compatibility
         * with the existing authorization flow.
         *
         * @param exception The error that prevented successful authorization.
         */
        private fun failAuth(exception: Throwable) {
            // Fail our own deferred (new mechanism)
            pendingDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    Timber.tag(TAG).w("Failing deferred: ${exception.message}")
                    deferred.completeExceptionally(exception)
                }
            }
            pendingDeferred = null

            // Also notify the legacy DiscordAuth companion (backward compatibility)
            DiscordAuth.failAuth(exception)
        }
    }
}
