package com.kuromusic.discord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

private const val DEFAULT_GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
private const val TAG = "DiscordSvc"

// ===========================================================================
// Gateway Opcodes
// ===========================================================================

enum class GatewayOpcode(val value: Int) {
    DISPATCH(0),
    HEARTBEAT(1),
    IDENTIFY(2),
    PRESENCE_UPDATE(3),
    VOICE_STATE_UPDATE(4),
    RESUME(6),
    RECONNECT(7),
    INVALID_SESSION(9),
    HELLO(10),
    HEARTBEAT_ACK(11),
}

// ===========================================================================
// Gateway Event types (sealed)
// ===========================================================================

sealed class GatewayEvent {
    data class Ready(
        val version: Int,
        val user: JSONObject,
        val sessionId: String,
        val resumeGatewayUrl: String,
    ) : GatewayEvent()
    data class Resumed(val userId: String) : GatewayEvent()
    data class InvalidSession(val canResume: Boolean) : GatewayEvent()
    data object Disconnected : GatewayEvent()
    data object RefreshToken : GatewayEvent()
}

private const val HEARTBEAT_BUFFER_CAPACITY = 64

class DiscordGateway(
    private val scope: CoroutineScope,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
) {
    private val _events = MutableSharedFlow<GatewayEvent>(
        replay = 0,
        extraBufferCapacity = HEARTBEAT_BUFFER_CAPACITY,
    )
    val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    @Volatile
    var currentSeq: Int = 0
        private set
    @Volatile
    var sessionId: String? = null
        private set
    @Volatile
    var isReady: Boolean = false
        private set

    private val webSocketId = AtomicLong(0)
    @Volatile
    private var webSocket: WebSocket? = null
    private var nextGatewayUrl: String = DEFAULT_GATEWAY_URL
    private var resumeGatewayUrl: String? = null
    private var lastIdentifyToken: String? = null

    private var heartbeatIntervalMs: Long = 41250L
    private var heartbeatJob: Job? = null
    private var heartbeatAckReceived: Boolean = true

    private var reconnectAttempts: Int = 0
    private var reconnectScheduled: Boolean = false

    fun setGatewayUrl(url: String) { nextGatewayUrl = url }

    fun connect(token: String) {
        lastIdentifyToken = token
        reconnectAttempts = 0
        reconnectScheduled = false
        doConnect()
    }

    fun connect() {
        if (lastIdentifyToken == null) {
            Timber.tag(TAG).w("connect() called without prior credentials, ignoring")
            return
        }
        reconnectAttempts = 0
        reconnectScheduled = false
        doConnect()
    }

    fun close() {
        reconnectScheduled = false
        isReady = false
        closeHttp(1000, "Client closing")
    }

    fun closeHttp(code: Int = 1000, reason: String = "") {
        stopHeartbeat()
        webSocket?.close(code, reason)
        webSocket = null
    }

    fun send(frame: JSONObject) {
        val ws = webSocket
        if (ws == null) {
            Timber.tag(TAG).w("WebSocket closed — dropping frame op=%d", frame.optInt("op", -1))
            return
        }
        Timber.tag(TAG).d("Sending op=%d", frame.optInt("op", -1))
        ws.send(frame.toString())
    }

    fun identify(token: String) {
        lastIdentifyToken = token
        sessionId = null
        resumeGatewayUrl = null
        val payload = JSONObject().apply {
            put("op", GatewayOpcode.IDENTIFY.value)
            put("d", JSONObject().apply {
                put("token", token)
                put("properties", JSONObject().apply {
                    put("os", "Android")
                    put("browser", "KuroMusic")
                    put("device", "KuroMusic")
                })
                put("presence", JSONObject().apply {
                    put("status", "online")
                    put("since", 0)
                    put("activities", JSONArray())
                    put("afk", false)
                })
                put("compress", false)
            })
        }
        send(payload)
    }

    fun heartbeat(sequence: Int? = null) {
        val payload = JSONObject().apply {
            put("op", GatewayOpcode.HEARTBEAT.value)
            put("d", sequence ?: currentSeq)
        }
        send(payload)
    }

    fun presenceUpdate(activity: JSONObject, status: String = "online", afk: Boolean = false, since: Long = 0L) {
        val payload = JSONObject().apply {
            put("op", GatewayOpcode.PRESENCE_UPDATE.value)
            put("d", JSONObject().apply {
                put("since", since)
                put("activities", JSONArray(listOf(activity)))
                put("status", status)
                put("afk", afk)
            })
        }
        send(payload)
    }

    private fun doConnect() {
        stopHeartbeat()
        val id = webSocketId.incrementAndGet()
        val url = resumeGatewayUrl ?: nextGatewayUrl
        Timber.tag(TAG).d("Connecting to %s (id=%d)", url, id)
        val request = Request.Builder().url(url).build()
        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Timber.tag(TAG).d("WebSocket opened (id=%d)", id)
                if (!verifyActive(id)) return
                webSocket = ws
            }
            override fun onMessage(ws: WebSocket, text: String) {
                if (!verifyActive(id)) return
                handleMessage(text)
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).d("WebSocket closing code=%d reason=%s (id=%d)", code, reason, id)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).d("WebSocket closed code=%d reason=%s (id=%d)", code, reason, id)
                if (!verifyActive(id)) return
                handleClose(code, reason)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val reason = t.message ?: "Unknown failure"
                Timber.tag(TAG).e(t, "WebSocket failure (id=%d) http=%s", id, response?.code ?: -1)
                if (!verifyActive(id)) return
                handleClose(-1, reason)
            }
        })
        webSocket = ws
    }

    private fun verifyActive(expectedId: Long): Boolean {
        val current = webSocketId.get()
        if (current != expectedId) {
            Timber.tag(TAG).w("Stale callback for id=%d, current=%d — ignoring", expectedId, current)
            return false
        }
        return true
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val op = json.optInt("op", -1)
            val seq = json.optInt("s", -1)
            if (seq >= 0) currentSeq = seq

            when (op) {
                GatewayOpcode.HELLO.value -> {
                    heartbeatIntervalMs = json.getJSONObject("d").getLong("heartbeat_interval")
                    startHeartbeat()
                    val token = lastIdentifyToken ?: return
                    if (sessionId != null) {
                        Timber.tag(TAG).d("Sending RESUME after HELLO (seq=%d)", currentSeq)
                        val payload = JSONObject().apply {
                            put("op", GatewayOpcode.RESUME.value)
                            put("d", JSONObject().apply {
                                put("token", token)
                                put("session_id", sessionId)
                                put("seq", currentSeq)
                            })
                        }
                        send(payload)
                    } else {
                        identify(token)
                    }
                }
                GatewayOpcode.HEARTBEAT_ACK.value -> {
                    heartbeatAckReceived = true
                }
                GatewayOpcode.RECONNECT.value -> {
                    Timber.tag(TAG).i("Received OP 7 RECONNECT from gateway")
                    scheduleReconnect()
                }
                GatewayOpcode.INVALID_SESSION.value -> {
                    val canResume = json.optBoolean("d", false)
                    Timber.tag(TAG).w("INVALID_SESSION canResume=%b", canResume)
                    isReady = false
                    if (canResume) {
                        val token = lastIdentifyToken ?: return
                        val payload = JSONObject().apply {
                            put("op", GatewayOpcode.RESUME.value)
                            put("d", JSONObject().apply {
                                put("token", token)
                                put("session_id", sessionId ?: "")
                                put("seq", currentSeq)
                            })
                        }
                        send(payload)
                    } else {
                        sessionId = null
                        resumeGatewayUrl = null
                        val token = lastIdentifyToken ?: return
                        identify(token)
                    }
                }
                GatewayOpcode.DISPATCH.value -> {
                    val t = json.optString("t", "")
                    val d = json.optJSONObject("d")
                    when (t) {
                        "READY" -> {
                            val user = d?.optJSONObject("user") ?: JSONObject()
                            val sid = d?.optString("session_id", "") ?: ""
                            val resumeUrl = d?.optString("resume_gateway_url", "") ?: ""
                            sessionId = sid
                            resumeGatewayUrl = resumeUrl
                            isReady = true
                            emitEvent(GatewayEvent.Ready(
                                version = d?.optInt("v", 0) ?: 0,
                                user = user,
                                sessionId = sid,
                                resumeGatewayUrl = resumeUrl,
                            ))
                        }
                        "RESUMED" -> {
                            isReady = true
                            emitEvent(GatewayEvent.Resumed(userId = ""))
                        }
                        "SESSIONS_REPLACE" -> {
                            emitEvent(GatewayEvent.RefreshToken)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error handling gateway message")
        }
    }

    private fun emitEvent(event: GatewayEvent) {
        try { _events.tryEmit(event) } catch (_: Exception) {}
    }

    private fun handleClose(code: Int, reason: String) {
        stopHeartbeat()
        webSocket = null
        isReady = false

        val action = DiscordReconnectDecider.decide(code, reason)
        when (action) {
            ReconnectAction.DISCONNECT -> {
                currentSeq = 0
                sessionId = null
                resumeGatewayUrl = null
                emitEvent(GatewayEvent.Disconnected)
            }
            ReconnectAction.RESUME -> {
                // Keep currentSeq, sessionId, resumeGatewayUrl for RESUME
                scheduleReconnect()
            }
            ReconnectAction.RECONNECT -> {
                currentSeq = 0
                sessionId = null
                resumeGatewayUrl = null
                scheduleReconnect()
            }
            ReconnectAction.REFRESH_TOKEN -> {
                currentSeq = 0
                sessionId = null
                resumeGatewayUrl = null
                emitEvent(GatewayEvent.RefreshToken)
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectScheduled) return
        reconnectScheduled = true
        val delayMs = reconnectBackoff()
        Timber.tag(TAG).d("Scheduling reconnect in %d ms (attempt %d)", delayMs, reconnectAttempts)
        reconnectAttempts++
        scope.launch {
            delay(delayMs)
            reconnectScheduled = false
            doConnect()
        }
    }

    private fun reconnectBackoff(): Long {
        val baseMs = 1000L
        val maxMs = 120_000L
        val jitter = Random.nextLong(0, 1000)
        val exponential = min(baseMs * (2.0).pow(reconnectAttempts).toLong(), maxMs)
        return exponential + jitter
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatAckReceived = true
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatIntervalMs)
                if (!heartbeatAckReceived) {
                    Timber.tag(TAG).w("Heartbeat not acked — reconnecting")
                    closeHttp(4000, "heartbeat timeout")
                    scheduleReconnect()
                    return@launch
                }
                heartbeatAckReceived = false
                heartbeat()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
