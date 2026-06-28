package com.kuromusic.discord

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import timber.log.Timber
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted token storage for Discord authentication data.
 *
 * Uses Android Keystore with AES/GCM/NoPadding to encrypt tokens before
 * persisting them in SharedPreferences named "discord_token".
 *
 * All encryption and decryption is delegated to the internal [AesKeystore] object.
 */
object DiscordTokenStore {

    private const val TAG = "DiscordSvc"
    private const val PREFS_NAME = "discord_token"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_DEVICE_VENDOR_ID = "device_vendor_id"
    private const val KEY_CLIENT_UUID = "client_uuid"

    private var prefs: SharedPreferences? = null
    private var initialized = false

    /**
     * Initializes the token store. Must be called once before any other method.
     *
     * @param context Application or Activity context (application context is retained).
     */
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        initialized = true
        Timber.tag(TAG).d("DiscordTokenStore initialized")
    }

    // -----------------------------------------------------------------------
    // Encryption helpers
    // -----------------------------------------------------------------------

    /**
     * Encrypts a plaintext string using AES/GCM via [AesKeystore].
     * Returns a Base64-encoded string, or null if the input is null.
     */
    fun encrypt(value: String?): String? {
        if (value == null) return null
        return try {
            AesKeystore.encrypt(value)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Encryption failed")
            null
        }
    }

    /**
     * Decrypts an encrypted Base64 string back to plaintext via [AesKeystore].
     * Returns null if the input is null or decryption fails.
     */
    fun decrypt(value: String?): String? {
        if (value == null) return null
        return try {
            AesKeystore.decrypt(value)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Decryption failed")
            null
        }
    }

    // -----------------------------------------------------------------------
    // Store methods
    // -----------------------------------------------------------------------

    /**
     * Stores the full OAuth2 token set (access token, refresh token, and expiration).
     *
     * @param accessToken  The OAuth2 access token.
     * @param refreshToken The OAuth2 refresh token.
     * @param expiresInSec Number of seconds until the access token expires.
     */
    fun storeFull(accessToken: String, refreshToken: String, expiresInSec: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSec * 1000L)
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            ?.putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
            ?.putString(KEY_EXPIRES_AT, encrypt(expiresAt.toString()))
            ?.apply()
        Timber.tag(TAG).d("Full token set stored (expires in ${expiresInSec}s)")
    }

    /**
     * Stores only the access token.
     *
     * @param accessToken The OAuth2 access token to store.
     */
    fun storeAccessToken(accessToken: String) {
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            ?.apply()
        Timber.tag(TAG).d("Access token stored")
    }

    /**
     * Stores a raw token string under the access token key.
     *
     * @param token The token string to store.
     */
    fun store(token: String) {
        storeAccessToken(token)
    }

    // -----------------------------------------------------------------------
    // Retrieve methods
    // -----------------------------------------------------------------------

    /**
     * Retrieves the stored access token (synchronous, may block on disk I/O).
     *
     * @return The decrypted access token, or null if not stored.
     */
    fun retrieve(): String? {
        val encrypted = prefs?.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return decrypt(encrypted)
    }

    /**
     * Suspending version of [retrieve]. Internally delegates to [retrieve]
     * since SharedPreferences reads may involve disk I/O.
     */
    suspend fun retrieveSuspend(): String? {
        return retrieve()
    }

    /**
     * Retrieves the stored refresh token.
     *
     * @return The decrypted refresh token, or null if not stored.
     */
    fun getRefreshToken(): String? {
        val encrypted = prefs?.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return decrypt(encrypted)
    }

    /**
     * Retrieves the expiration timestamp (epoch milliseconds) of the access token.
     *
     * @return The expiration time in milliseconds, or null if not stored.
     */
    fun getExpiresAt(): Long? {
        val encrypted = prefs?.getString(KEY_EXPIRES_AT, null) ?: return null
        val decrypted = decrypt(encrypted) ?: return null
        return decrypted.toLongOrNull()
    }

    // -----------------------------------------------------------------------
    // Device identifiers
    // -----------------------------------------------------------------------

    /**
     * Returns a persistent device vendor ID, generating and storing a new
     * UUID if none exists.
     */
    fun getDeviceVendorId(): String {
        val existing = prefs?.getString(KEY_DEVICE_VENDOR_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        prefs?.edit()?.putString(KEY_DEVICE_VENDOR_ID, newId)?.apply()
        Timber.tag(TAG).d("Generated new device vendor ID")
        return newId
    }

    /**
     * Returns a persistent client UUID, generating and storing a new
     * UUID if none exists.
     */
    fun getClientUuid(): String {
        val existing = prefs?.getString(KEY_CLIENT_UUID, null)
        if (!existing.isNullOrBlank()) return existing

        val newUuid = UUID.randomUUID().toString()
        prefs?.edit()?.putString(KEY_CLIENT_UUID, newUuid)?.apply()
        Timber.tag(TAG).d("Generated new client UUID")
        return newUuid
    }

    // -----------------------------------------------------------------------
    // Clear
    // -----------------------------------------------------------------------

    /**
     * Removes all stored token data and device identifiers.
     */
    fun clear() {
        prefs?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_REFRESH_TOKEN)
            ?.remove(KEY_EXPIRES_AT)
            ?.remove(KEY_DEVICE_VENDOR_ID)
            ?.remove(KEY_CLIENT_UUID)
            ?.apply()
        Timber.tag(TAG).d("All Discord token data cleared")
    }

    // -----------------------------------------------------------------------
    // Internal AES/GCM Keystore
    // -----------------------------------------------------------------------

    /**
     * AES/GCM/NoPadding encryption using the Android Keystore.
     *
     * The key is stored securely in the device's hardware-backed keystore
     * (or software fallback on older devices) under the alias
     * "kuromusic_discord_token_key".
     *
     * Thread safety for keystore loading is provided via [getKeyStore].
     */
    object AesKeystore {

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "kuromusic_discord_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128 // bits

        /**
         * Encrypts a plaintext string.
         *
         * @param plaintext The text to encrypt.
         * @return Base64-encoded (IV + ciphertext + GCM tag).
         */
        fun encrypt(plaintext: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

            val iv = cipher.iv // 12 bytes generated by the Cipher
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Concatenate IV + ciphertext (tag is appended by GCM mode)
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            return Base64.getEncoder().encodeToString(combined)
        }

        /**
         * Decrypts an encrypted string previously produced by [encrypt].
         *
         * @param encrypted Base64-encoded (IV + ciphertext + GCM tag).
         * @return The original plaintext string.
         */
        fun decrypt(encrypted: String): String {
            val combined = Base64.getDecoder().decode(encrypted)

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        }

        /**
         * Retrieves the secret key from Android Keystore, generating it if
         * it does not yet exist.
         */
        private fun getOrCreateKey(): SecretKey {
            val keyStore = getKeyStore()
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }

            // Generate a new AES key in the Android Keystore
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE,
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            Timber.tag(TAG).d("Generated new AES-256 GCM key in Android Keystore")
            return keyGenerator.generateKey()
        }

        /**
         * Thread-safe access to the Android KeyStore instance.
         * Uses @Synchronized to prevent concurrent keystore loading.
         */
        @Synchronized
        @VisibleForTesting
        fun getKeyStore(): KeyStore {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            return keyStore
        }
    }
}
