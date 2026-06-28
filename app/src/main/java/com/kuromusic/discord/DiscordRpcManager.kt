package com.kuromusic.discord

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

data class DiscordRpcUser(
    val id: String,
    val username: String,
    val name: String,
    val avatar: String?,
)

enum class RpcStatus { Disconnected, Authorizing, Connected }

data class ActivityPayload(
    val name: String = "",
    val type: Int,
    val details: String? = null,
    val state: String? = null,
    val url: String? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val smallImage: String? = null,
    val smallText: String? = null,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val buttons: List<Pair<String, String>> = emptyList(),
    val applicationId: String? = null,
)

object DiscordRpcManager {
    private const val TAG = "DiscordSvc"

    @Volatile private var initialized = false
    @Volatile private var ready = false
    @Volatile private var authorized = false
    @Volatile private var accessToken: String? = null
    @Volatile private var authorizeInProgress = false
    @Volatile private var lastActivitySentAtMs = 0L
    @Volatile private var lastActivity: ActivityPayload? = null
    @Volatile private var currentSongId: String? = null
    @Volatile private var currentIsPlaying = false
    private val currentActivityId = AtomicLong(0L)
    @Volatile private var imageResolutionJob: Job? = null
    @Volatile private var currentActivityHadImages = false
    private val reconnectMutex = Mutex()

    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    val accessTokenFlow: StateFlow<String?> = _accessTokenFlow.asStateFlow()

    private val _connectionStatus = MutableStateFlow(RpcStatus.Disconnected)
    val connectionStatus: StateFlow<RpcStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _currentUser = MutableStateFlow<DiscordRpcUser?>(null)
    val currentUser: StateFlow<DiscordRpcUser?> = _currentUser.asStateFlow()

    private val _lastToken = java.util.concurrent.atomic.AtomicReference<String?>(null)

    private val _settingsChanged = MutableStateFlow(0)
    val settingsChanged: StateFlow<Int> = _settingsChanged.asStateFlow()

    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = DiscordAuth()
    private lateinit var gateway: DiscordGateway

    private val appId: String get() = com.kuromusic.BuildConfig.DISCORD_APP_ID.toString()

    fun notifySettingsChanged() { _settingsChanged.value++; currentSongId = null; currentIsPlaying = false }

    fun getAccessToken(): String? = accessToken
    fun isInitialized(): Boolean = initialized
    fun isAuthorized(): Boolean = authorized
    fun isReady(): Boolean = ready

    fun isShowingSong(songId: String, isPlaying: Boolean): Boolean {
        if (currentSongId != songId || currentIsPlaying != isPlaying) return false
        if (lastActivity == null) return false
        if (currentActivityHadImages &&
            lastActivity?.largeImage == null && lastActivity?.smallImage == null &&
            (imageResolutionJob == null || imageResolutionJob?.isCompleted == true)
        ) return false
        return true
    }

    fun clearLastError() { _lastError.value = null }

    fun init(context: Context) {
        DiscordTokenStore.init(context.applicationContext)
        if (initialized && scope.isActive) return
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
        if (!::gateway.isInitialized) {
            gateway = DiscordGateway(scope)
        }
        initialized = true
        _connectionStatus.value = RpcStatus.Disconnected
        scope.launch {
            gateway.events.collect { event -> handleGatewayEvent(event) }
        }
        scope.launch {
            val saved = DiscordTokenStore.retrieveSuspend()
            if (!saved.isNullOrEmpty()) {
                reconnectWithToken(saved)
            }
        }
    }

    fun authorize(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (authorizeInProgress) {
            scope.launch(Dispatchers.Main) { onComplete(false) }
            return
        }
        authorizeInProgress = true

        if (ready && authorized) {
            authorizeInProgress = false
            scope.launch(Dispatchers.Main) { onComplete(true) }
            return
        }
        if (authorized) {
            authorizeInProgress = false
            reconnectWithToken(accessToken ?: "")
            scope.launch(Dispatchers.Main) { onComplete(true) }
            return
        }

        _connectionStatus.value = RpcStatus.Authorizing
        _lastError.value = null

        scope.launch {
            try {
                val result = auth.authorize(activity)
                DiscordTokenStore.storeFull(result.accessToken, result.refreshToken, result.expiresIn)
                accessToken = result.accessToken
                _accessTokenFlow.value = result.accessToken
                authorized = true

                reconnectMutex.withLock {
                    runCatching { gateway.close() }
                    gateway.connect(result.accessToken)
                }
                authorizeInProgress = false
                scope.launch(Dispatchers.Main) { onComplete(true) }
            } catch (_: UserCancelled) {
                _connectionStatus.value = RpcStatus.Disconnected
                authorizeInProgress = false
                scope.launch(Dispatchers.Main) { onComplete(false) }
            } catch (_: NoBrowser) {
                _connectionStatus.value = RpcStatus.Disconnected
                authorizeInProgress = false
                scope.launch(Dispatchers.Main) { onComplete(false) }
            } catch (e: Throwable) {
                _lastError.value = "discord_error_loopback_timeout"
                _connectionStatus.value = RpcStatus.Disconnected
                authorized = false
                ready = false
                authorizeInProgress = false
                scope.launch(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun cancelAuthorize() { auth.cancel() }

    fun setToken(token: String) {
        if (!initialized) return
        _lastToken.set(token)
        authorized = true
        ready = false
        accessToken = token
        _accessTokenFlow.value = token
        DiscordTokenStore.store(token)
        scope.launch {
            reconnectMutex.withLock {
                runCatching { gateway.close() }
                _connectionStatus.value = RpcStatus.Authorizing
                _lastError.value = null
                gateway.connect(token)
            }
        }
    }

    fun setActivity(
        name: String,
        type: Int = 2,
        details: String? = null,
        state: String? = null,
        largeImage: String? = null,
        largeText: String? = null,
        smallImage: String? = null,
        smallText: String? = null,
        startMs: Long? = null,
        endMs: Long? = null,
        buttons: List<Pair<String, String>> = emptyList(),
        songId: String? = null,
        isPlaying: Boolean = true,
    ) {
        scope.launch {
            reconnectMutex.withLock {
                if (!ready) return@withLock

                val stateChanged = songId != currentSongId || isPlaying != currentIsPlaying
                val now = System.currentTimeMillis()
                if (!stateChanged && lastActivitySentAtMs > 0L && (now - lastActivitySentAtMs) < 2000L) return@withLock
                lastActivitySentAtMs = now

                currentSongId = songId
                currentIsPlaying = isPlaying
                currentActivityId.incrementAndGet()
                currentActivityHadImages = !largeImage.isNullOrEmpty() || !smallImage.isNullOrEmpty()

                val payload = ActivityPayload(
                    name = name, type = type,
                    details = details, state = state,
                    url = null,
                    largeImage = largeImage, largeText = largeText,
                    smallImage = smallImage, smallText = smallText,
                    startMs = startMs, endMs = endMs,
                    buttons = buttons,
                    applicationId = appId,
                )
                lastActivity = payload

                try {
                    gateway.presenceUpdate(DiscordPresence.buildActivity(payload))
                } catch (_: Exception) { }
            }
        }

        imageResolutionJob?.cancel()
        val currentToken = accessToken ?: return
        if (largeImage.isNullOrEmpty() && smallImage.isNullOrEmpty()) return

        val activityIdAtLaunch = currentActivityId.get()
        imageResolutionJob = scope.launch {
            val tokenHeader = "Bearer $currentToken"
            val largeResolved = if (!largeImage.isNullOrEmpty()) DiscordExternalAssets.resolve(largeImage, appId, tokenHeader) else null
            val smallResolved = if (!smallImage.isNullOrEmpty()) DiscordExternalAssets.resolve(smallImage, appId, tokenHeader) else null
            if (largeResolved == null && smallResolved == null) return@launch
            if (activityIdAtLaunch != currentActivityId.get()) return@launch

            val resolvedPayload = ActivityPayload(
                name = name, type = type,
                details = details, state = state,
                url = null,
                largeImage = largeResolved, largeText = largeText,
                smallImage = smallResolved, smallText = smallText,
                startMs = startMs, endMs = endMs,
                buttons = buttons,
                applicationId = appId,
            )
            lastActivity = resolvedPayload
            try {
                gateway.presenceUpdate(DiscordPresence.buildActivity(resolvedPayload))
            } catch (_: Exception) { }
        }
    }

    fun clear() {
        scope.launch {
            reconnectMutex.withLock {
                if (!ready) return@withLock
                lastActivity = null
                currentSongId = null
                currentIsPlaying = false
                currentActivityHadImages = false
                currentActivityId.incrementAndGet()
                imageResolutionJob?.cancel()
                try {
                    val emptyActivity = DiscordPresence.buildActivity(ActivityPayload(name = "", type = 2))
                    emptyActivity.put("name", "KuroMusic")
                    gateway.presenceUpdate(emptyActivity)
                } catch (_: Exception) { }
            }
        }
    }

    fun reconnectWithToken(token: String) {
        if (!initialized) return
        scope.launch {
            reconnectMutex.withLock {
                accessToken = token
                _accessTokenFlow.value = token
                DiscordTokenStore.store(token)
                _connectionStatus.value = RpcStatus.Authorizing
                try {
                    runCatching { gateway.close() }
                    gateway.connect(token)
                } catch (_: Throwable) {
                    _connectionStatus.value = RpcStatus.Disconnected
                }
            }
        }
    }

    fun disconnect() {
        currentActivityId.incrementAndGet()
        imageResolutionJob?.cancel()
        runCatching { gateway.close() }
        _connectionStatus.value = RpcStatus.Disconnected
        ready = false
        authorized = false
        currentSongId = null
        currentIsPlaying = false
        currentActivityHadImages = false
    }

    fun destroy() {
        currentActivityId.incrementAndGet()
        imageResolutionJob?.cancel()
        runCatching { gateway.close() }
        scope.cancel()
        ready = false
        authorized = false
        initialized = false
        _connectionStatus.value = RpcStatus.Disconnected
        lastActivity = null
        currentSongId = null
        currentIsPlaying = false
        currentActivityHadImages = false
    }

    fun logout() {
        disconnect()
        accessToken = null
        _accessTokenFlow.value = null
        _currentUser.value = null
        DiscordTokenStore.clear()
        _lastError.value = null
        lastActivity = null
        currentActivityHadImages = false
    }

    private suspend fun handleGatewayEvent(event: GatewayEvent) {
        when (event) {
            is GatewayEvent.Ready -> {
                ready = true
                authorized = true
                _connectionStatus.value = RpcStatus.Connected
                _lastError.value = null
                val token = accessToken ?: return
                fetchCurrentUser(token)?.let { _currentUser.value = it }
                // Reenviar última actividad si existe
                resendLastActivity()
            }
            is GatewayEvent.Resumed -> {
                ready = true
                authorized = true
                _connectionStatus.value = RpcStatus.Connected
                _lastError.value = null
                // Reenviar última actividad si existe
                resendLastActivity()
            }
            is GatewayEvent.Disconnected -> {
                ready = false
                authorized = false
                _connectionStatus.value = RpcStatus.Disconnected
                currentSongId = null
                currentIsPlaying = false
                imageResolutionJob?.cancel()
            }
            is GatewayEvent.InvalidSession -> {
                imageResolutionJob?.cancel()
            }
            is GatewayEvent.RefreshToken -> {
                refreshAndReconnect()
            }
        }
    }

    private fun resendLastActivity() {
        val activity = lastActivity ?: return
        try {
            gateway.presenceUpdate(DiscordPresence.buildActivity(activity))
        } catch (_: Exception) { }
    }

    private suspend fun refreshAndReconnect() {
        val refreshToken = DiscordTokenStore.getRefreshToken() ?: run { logout(); return }
        try {
            val refreshed = auth.refresh(refreshToken)
            DiscordTokenStore.storeFull(refreshed.accessToken, refreshed.refreshToken, refreshed.expiresIn)
            reconnectWithToken(refreshed.accessToken)
        } catch (_: InvalidGrant) { logout() }
        catch (_: Throwable) { }
    }

    private fun fetchCurrentUser(token: String): DiscordRpcUser? {
        return try {
            val url = java.net.URL("https://discord.com/api/v10/users/@me")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            val body = if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().readText() else return null
            conn.disconnect()
            val json = JSONObject(body)
            val id = json.getString("id")
            val username = json.getString("username")
            val name = json.optString("global_name", username)
            val avatarHash = json.optString("avatar")
            val avatar = if (avatarHash.isNotEmpty() && avatarHash != "null") "https://cdn.discordapp.com/avatars/$id/$avatarHash.png" else null
            DiscordRpcUser(id, username, name, avatar)
        } catch (_: Exception) { null }
    }
}
