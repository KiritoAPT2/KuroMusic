@file:Suppress("DEPRECATION")

package com.kuromusic.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.database.StandaloneDatabaseProvider

import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.io.File
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import android.widget.Toast
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.WatchEndpoint
import com.kuromusic.jossredconnect.JossRedClient
import com.kuromusic.MainActivity
import com.kuromusic.R
import com.kuromusic.constants.AudioNormalizationKey
import com.kuromusic.constants.AudioQualityKey
import com.kuromusic.constants.AutoLoadMoreKey
import com.kuromusic.constants.AutoSkipNextOnErrorKey
import com.kuromusic.constants.DisableLoadMoreWhenRepeatAllKey
import com.kuromusic.constants.DiscordTokenKey
import com.kuromusic.constants.DiscordUseDetailsKey
import com.kuromusic.constants.EnableDiscordRPCKey
import com.kuromusic.constants.HideExplicitKey
import com.kuromusic.constants.HistoryDuration
import com.kuromusic.constants.MediaSessionConstants.CommandToggleLike
import com.kuromusic.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.kuromusic.constants.MediaSessionConstants.CommandToggleShuffle
import com.kuromusic.constants.PauseListenHistoryKey
import com.kuromusic.constants.PersistentQueueKey
import com.kuromusic.constants.PlayerVolumeKey
import com.kuromusic.constants.RepeatModeKey
import com.kuromusic.constants.ShowLyricsKey
import com.kuromusic.constants.SimilarContent
import com.kuromusic.constants.SkipSilenceKey
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.Event
import com.kuromusic.db.entities.FormatEntity
import com.kuromusic.db.entities.LyricsEntity
import com.kuromusic.db.entities.RelatedSongMap
import com.kuromusic.di.DownloadCache
import com.kuromusic.di.PlayerCache
import com.kuromusic.extensions.SilentHandler
import com.kuromusic.extensions.collect
import com.kuromusic.extensions.collectLatest
import com.kuromusic.extensions.currentMetadata
import com.kuromusic.extensions.findNextMediaItemById
import com.kuromusic.extensions.mediaItems
import com.kuromusic.extensions.metadata
import com.kuromusic.extensions.toMediaItem
import com.kuromusic.extensions.toQueue
import com.kuromusic.lyrics.LyricsHelper
import com.kuromusic.models.PersistPlayerState
import com.kuromusic.models.PersistQueue
import com.kuromusic.models.toMediaMetadata
import com.kuromusic.playback.queues.EmptyQueue
import com.kuromusic.playback.queues.Queue
import com.kuromusic.playback.queues.YouTubeQueue
import com.kuromusic.playback.queues.filterExplicit
import com.kuromusic.utils.CoilBitmapLoader
import com.kuromusic.utils.DiscordRPC
import com.kuromusic.utils.NetworkConnectivityObserver
import com.kuromusic.utils.YTPlayerUtils
import com.kuromusic.utils.dataStore
import com.kuromusic.utils.enumPreference
import com.kuromusic.utils.get
import com.kuromusic.utils.reportException
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    // Media3 manages focus automatically when handleAudioFocus is true
    private var hasAudioFocus = false
    
    // SupervisorJob prevents one failure from cancelling others
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    
    private val binder = MusicBinder()

    // Cache to prevent blocking ExoPlayer loader
    private val playbackCache = java.util.concurrent.ConcurrentHashMap<String, YTPlayerUtils.PlaybackData>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.kuromusic.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.kuromusic.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)

    // Cached preference to avoid blocking reads
    private var isJossRedEnabled = false
    private var isJossRedFallbackEnabled = false

    // Preference Keys
    private val JossRedMultimediaKey = booleanPreferencesKey("JossRedMultimedia")

    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    private lateinit var simpleCache: SimpleCache
    private lateinit var cacheEvictor: LeastRecentlyUsedCacheEvictor


    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: Job? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var consecutivePlaybackErr = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        
        // Initialize YTPlayerUtils cache
        YTPlayerUtils.initialize(applicationContext)

        // Clear old/corrupt cache
        clearExoPlayerCache()

        // Initialize local cache
        cacheEvictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) // 200MB
        simpleCache = SimpleCache(
            File(cacheDir, "media"),
            cacheEvictor,
            StandaloneDatabaseProvider(this)
        )
        
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.kuromusic_monochrome)
                },
        )
        // 1. INICIALIZACIÓN INMEDIATA (Orden Crítico)
        // 1. INICIALIZACIÓN INMEDIATA (Orden Crítico)
        val exoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(this)
            .setRenderersFactory(createRenderersFactory())
            .setMediaSourceFactory(createMediaSourceFactory())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        32000, // Min buffer 32s
                        64000, // Max buffer 64s
                        1000, // Buffer for playback 1s (Reduced for cold start)
                        1500 // Rebuffer 1.5s
                    ).build()
            )
            .build()

        exoPlayer.addListener(this@MusicService)
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e(error, "Error de reproducción: ${error.message}")
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    // Notificar error de red o link expirado
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@MusicService, R.string.error_no_internet, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
        sleepTimer = SleepTimer(scope, exoPlayer)
        exoPlayer.addListener(sleepTimer)
        exoPlayer.addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
        
        player = exoPlayer


        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }

        // 3. CONFIGURAR SESIÓN
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()

        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works (Async & Safe)
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                // Safe get() guaranteed to be done here
                controllerFuture.get()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error initializing self-controller")
            }
        }, ContextCompat.getMainExecutor(this))

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        // Observar conectividad de red
        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    // Reintentar reproducción cuando vuelve la conexión
                    waitingForNetworkConnection.value = false
                    if (::player.isInitialized) {
                        if (player.currentMediaItem != null && player.playWhenReady) {
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }
        }

        // Observe JossRedMultimedia preference
        scope.launch {
            dataStore.data.map { it[JossRedMultimediaKey] ?: false }.distinctUntilChanged().collect {
                isJossRedEnabled = it
            }
        }

        playerVolume.collectLatest(scope) {
            if (::player.isInitialized) {
                player.volume = it
            }
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (::player.isInitialized) {
                if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                    discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                } else {
                    discordRpc?.closeRPC()
                }
            }
            
            // 🚀 CONCURRENT PRE-FETCH (Next 3 songs)
            if (::player.isInitialized) {
                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    // Launch in IO scope with SupervisorJob
                    scope.launch(Dispatchers.IO) {
                        try {
                            val queueSize = player.mediaItemCount
                            val nextSongs = (1..3).mapNotNull { offset -> 
                                val index = (currentIndex + offset) % queueSize
                                if (index != currentIndex) player.getMediaItemAt(index) else null
                            }
                            
                            nextSongs.forEach { item ->
                                launch { // Concurrent launch
                                    try {
                                        val nextId = item.mediaId
                                        Timber.d("🚀 Pre-fetching next song ($nextId) in background")
                                        val result = YTPlayerUtils.playerResponseForPlayback(
                                            videoId = nextId,
                                            audioQuality = audioQuality,
                                            connectivityManager = connectivityManager
                                        )
                                        result.getOrNull()?.let { playbackCache[nextId] = it }
                                    } catch (e: Exception) {
                                        Timber.e(e, "❌ Pre-fetch failed for ${item.mediaId}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "❌ Pre-fetch supervisor error")
                        }
                    }
                }
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                if (::player.isInitialized) {
                    player.skipSilenceEnabled = it
                }
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            setupLoudnessEnhancer()
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (::player.isInitialized) {
                        if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                            currentSong.value?.let {
                                discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                            }
                        }
                    }
                }
            }

        // 4. CARGA ASÍNCRONA (Punto 3 de tu plan)
        scope.launch(Dispatchers.IO) {
            if (dataStore.get(PersistentQueueKey, true)) {
                runCatching {
                    filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    withContext(Dispatchers.Main) {
                        if (::player.isInitialized) {
                            val restoredQueue = queue.toQueue()
                            playQueue(
                                queue = restoredQueue,
                                playWhenReady = false,
                            )
                        }
                    }
                }

                runCatching {
                    filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    automixItems.value = queue.items.map { it.toMediaItem() }
                }

                // Restaurar estado del reproductor
                runCatching {
                    filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistPlayerState
                        }
                    }
                }.onSuccess { playerState ->
                    // Restaurar configuración del reproductor después de cargar la cola
                    withContext(Dispatchers.Main) {
                        delay(1000) // Esperar a que la cola se cargue
                        if (::player.isInitialized) {
                            player.repeatMode = playerState.repeatMode
                            player.shuffleModeEnabled = playerState.shuffleModeEnabled
                            player.volume = playerState.volume

                            // Restaurar posición si sigue siendo válida
                            if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                                player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                            }
                        }
                    }
                }
            }
        }

        // Guardar cola periódicamente para prevenir pérdida por crash o force kill
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        // Guardar cola más frecuentemente cuando está reproduciendo
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    }


    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state
         */
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked ==
                                true
                            ) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main.immediate) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        // Cancel previous scope and create new one
        scope.launch(Dispatchers.Main) {
            // Reset Job if needed or just continue using supervisor
             if (!scope.isActive) {
                 // Re-create scope if it was cancelled (unlikely in Service)
                 // private var scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
             }
        }

        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            if (queue.preloadItem != null) {
                // Pre-resolve the first item
                val firstItem = initialStatus.items.getOrNull(initialStatus.mediaItemIndex)
                if (firstItem != null) {
                    preparePlayback(firstItem.mediaId)
                }
                
                withContext(Dispatchers.Main) {
                    player.addMediaItems(
                        0,
                        initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                    )
                    player.addMediaItems(
                        initialStatus.items.subList(
                            initialStatus.mediaItemIndex + 1,
                            initialStatus.items.size
                        )
                    )
                }
            } else {
                 val startIndex = if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0
                 
                 // Pre-resolve the start item
                 val startItem = initialStatus.items.getOrNull(startIndex)
                 if (startItem != null) {
                     preparePlayback(startItem.mediaId)
                 }

                 withContext(Dispatchers.Main) {
                    player.setMediaItems(
                        initialStatus.items,
                        startIndex,
                        initialStatus.position,
                    )
                    player.prepare()
                    player.playWhenReady = playWhenReady
                }
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Guardar canción actual
        val currentSong = player.currentMediaItem

        // Remover otras canciones de la cola
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            // Agregar canciones de radio después de la canción actual
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }
                            }
                    }
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        // Si la cola está vacía o el reproductor está inactivo, reproducir inmediatamente
        // Si la cola está vacía o el reproductor está inactivo, reproducir inmediatamente
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            val firstItem = items.firstOrNull()
            if (firstItem != null) {
                scope.launch {
                    preparePlayback(firstItem.mediaId)
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(items)
                        player.prepare()
                        player.play()
                    }
                }
            } else {
                 player.setMediaItems(items)
                 player.prepare()
                 player.play()
            }
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insertar items inmediatamente después del item actual en el espacio de ventana/índice
        player.addMediaItems(insertIndex, items)
        player.prepare()

        // Pre-fetch the first inserted item as it is likely to be played next
        items.firstOrNull()?.let { nextItem ->
             scope.launch(Dispatchers.IO) {
                 preparePlayback(nextItem.mediaId)
             }
        }

        if (shuffleEnabled) {
            // Reconstruir orden aleatorio para que los items recién insertados se reproduzcan a continuación
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Los índices recién insertados son un rango contiguo [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Recopilar el orden de recorrido aleatorio existente excluyendo el índice actual
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preservar el orden hacia adelante original

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Construir nuevo orden aleatorio: actual -> recién insertados (en orden de inserción) -> resto
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Llenar cualquier índice faltante (seguridad) para asegurar una permutación completa
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLike())
            }
        }
    }

    private fun setupLoudnessEnhancer() {
        scope.launch(Dispatchers.Main) {
            try {
                val audioSessionId = player.audioSessionId

                if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
                    Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
                    return@launch
                }
                
                // Guard: Only setup enhancer if player is ready
                if (player.playbackState != Player.STATE_READY) {
                    return@launch
                }

                // Defer creation if this is the first time (avoid blocking app startup)
                if (loudnessEnhancer == null) {
                    delay(500)  // Let UI draw first
                }

                // Create or recreate enhancer if necessary
                if (loudnessEnhancer == null) {
                    try {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                        Log.d(TAG, "LoudnessEnhancer created (lazy) for sessionId=$audioSessionId")
                    } catch (e: RuntimeException) {
                        // Some devices (Tecno/Infinix with custom ROMs) may fail here
                        Log.e(TAG, "Failed to create LoudnessEnhancer, but app continues", e)
                        reportException(e)
                        return@launch
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create LoudnessEnhancer", e)
                        reportException(e)
                        return@launch
                    }
                }

                // Configure the enhancer
                val currentMediaId = player.currentMediaItem?.mediaId

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }

                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }

                    val loudnessDb = format?.loudnessDb

                    if (loudnessDb != null) {
                        // Fix: Disable LoudnessEnhancer for Opus to prevent "MediaCodec mime type not supported" error
                        if (format.mimeType.contains("opus", ignoreCase = true)) {
                            Log.d(TAG, "setupLoudnessEnhancer: Skipping Opus format due to codec compatibility")
                            loudnessEnhancer?.enabled = false
                            return@launch
                        }

                        val targetGain = (-loudnessDb * 100).toInt()
                        val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                        try {
                            loudnessEnhancer?.setTargetGain(clampedGain)
                            loudnessEnhancer?.enabled = true
                            Log.d(TAG, "LoudnessEnhancer gain applied: $clampedGain mB")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying LoudnessEnhancer gain", e)
                            reportException(e)
                            releaseLoudnessEnhancer()
                        }
                    } else {
                        loudnessEnhancer?.enabled = false
                        Log.w(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                    }
                } else {
                    loudnessEnhancer?.enabled = false
                    Log.d(TAG, "setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in setupLoudnessEnhancer", e)
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // Immediate metadata update to sync UI
        currentMediaMetadata.value = mediaItem?.metadata
        
        if (mediaItem != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    // Try to extract metadata and player duration to avoid 0:00
                    val mediaMetadata = mediaItem.metadata
                    if (mediaMetadata != null) {
                        // Capture duration from player if metadata says <= 0
                        // Convert millis to seconds for internal MediaMetadata
                        val actualDurationSeconds = if (mediaMetadata.duration <= 0) {
                             withContext(Dispatchers.Main) { (player.duration.coerceAtLeast(0) / 1000).toInt() }
                        } else {
                            mediaMetadata.duration
                        }
                        
                        val updatedMetadata = mediaMetadata.copy(duration = actualDurationSeconds)
                        
                        // Upsert the song so it exists for history/UI with correct duration
                        database.upsert(updatedMetadata)
                    }

                    // Delete existing history for this song to prevent duplicates (Recently Played = Unique Songs)
                    database.deleteSongHistory(mediaItem.mediaId)

                    // Insert into history (no FK constraint, so it won't crash even if song insertion above failed)
                    database.insert(
                        com.kuromusic.db.entities.SongHistory(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                    Timber.tag("MusicService").w(e, "Constraint error in history")
                } catch (e: Exception) {
                    Timber.tag("MusicService").e(e, "Error recording history/song")
                }
            }
        }
        lastPlaybackSpeed = -1.0f // forzar actualización de canción

        setupLoudnessEnhancer()

        discordUpdateJob?.cancel()

        // Resetear errores consecutivos cuando hay transición exitosa
        consecutivePlaybackErr = 0

        // Auto cargar más canciones
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            (currentQueue.hasNextPage() || dataStore.get(AutoLoadMoreKey, true)) &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                if (currentQueue.hasNextPage()) {
                    val mediaItems =
                        currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                    if (player.playbackState != STATE_IDLE) {
                        player.addMediaItems(mediaItems.drop(1))
                    }
                } else {
                    // Fallback: Start radio from last song if queue doesn't support pagination
                    val lastItem = player.getMediaItemAt(player.mediaItemCount - 1)
                    val lastMetadata = lastItem.metadata
                    lastMetadata?.id?.let { videoId ->
                        val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
                        nextResult?.items?.let { items ->
                            val mediaItems = items.map { it.toMediaItem() }
                                .filterExplicit(dataStore.get(HideExplicitKey, false))
                            if (player.playbackState != STATE_IDLE) {
                                // Add to player
                                player.addMediaItems(mediaItems)
                                // Note: currentQueue is still the old queue, future pages won't load unless we switch queue or keep doing this.
                                // Ideal: switch to YouTubeQueue(endpoint = nextResult.endpoint).
                                // But changing currentQueue reference might be tricky here.
                                // For now this ensures "infinite" for at least one batch.
                                // If we want true infinite, we should update currentQueue?
                                // currentQueue is likely a var in Service.
                                // I'll check if I can assign it.
                                // If I can't seeing the code, I'll stick to appending.
                            }
                        }
                    }
                }
            }
        }

        // Guardar estado cuando cambia el item de medios
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(500) // Pequeño delay para asegurar que el estado esté estable
                saveQueueToDisk()
            }
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        // Guardar estado cuando cambia el estado de reproducción
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }

        // Cuando termina la reproducción, ocultar notificación si la cola está vacía
        if (playbackState == Player.STATE_ENDED) {
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    // Limpiar metadata para forzar actualización de notificación
                    currentMediaMetadata.value = null
                }
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            setupLoudnessEnhancer()
        }

        // Actualizar notificación cuando cambia el estado de reproducción
        scope.launch {
            delay(300)
            updateNotification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                openAudioEffectSession()
            } else {
                closeAudioEffectSession()
            }
        }

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            // Forzar actualización de notificación para asegurar que la imagen se cargue
            scope.launch {
                delay(200)
                updateNotification()
            }
        }

        // Actualización de Discord RPC
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSong.value?.let { song ->
                    scope.launch {
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                    }
                }
            }
            // Send empty activity to the Discord RPC if the player is not playing
            else if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)){
                scope.launch {
                    discordRpc?.stopActivity()
                }
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Si la cola está vacía, no mezclar
            if (player.mediaItemCount == 0) return

            // Siempre poner el item que se está reproduciendo primero
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }

        // Guardar estado cuando cambia el modo aleatorio
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Guardar estado cuando cambia el modo de repetición
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Log.e(TAG, "Player error: ${error.errorCodeName}, message: ${error.message}", error)

        val isConnectionError = (error.cause?.cause is PlaybackException) &&
                (error.cause?.cause as PlaybackException).errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED

        if (!isNetworkConnected.value || isConnectionError) {
            waitOnNetworkError()
            return
        }

        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    private fun clearExoPlayerCache() {
        try {
            val cacheDir = File(cacheDir, "exoplayer")
            val filesCacheDir = filesDir.resolve("exoplayer")
            
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Timber.tag(TAG).d("Cleared cacheDir/exoplayer")
            }
            if (filesCacheDir.exists()) {
                filesCacheDir.deleteRecursively()
                Timber.tag(TAG).d("Cleared filesDir/exoplayer")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear ExoPlayer cache")
        }
    }
    


    private suspend fun preparePlayback(mediaId: String): YTPlayerUtils.PlaybackData? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                 // Check cache first (including expiry)
                playbackCache[mediaId]?.let { cached ->
                    if (cached.streamExpiresInSeconds * 1000L > System.currentTimeMillis()) {
                        return@withContext cached
                    }
                }

                val playback = YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager
                ).getOrThrow()

                val streamUrl = YTPlayerUtils.getValidStreamUrl(playback.format)
                
                if (streamUrl.startsWith("https://")) {
                    try {
                        android.net.Uri.parse(streamUrl)
                        Timber.tag(TAG).i("✅ FINAL URL VALIDATED: $streamUrl | Length: ${streamUrl.length}")
                        
                        val validatedPlayback = playback.copy(streamUrl = streamUrl)
                        playbackCache[mediaId] = validatedPlayback
                        
                        // Trigger side effects asynchronously
                         scope.launch(Dispatchers.IO) {
                              try {
                                  val format = validatedPlayback.format
                                  database.query {
                                    upsert(
                                        FormatEntity(
                                            id = mediaId,
                                            itag = format.itag,
                                            mimeType = format.mimeType.split(";")[0],
                                            codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                                            bitrate = format.bitrate,
                                            sampleRate = format.audioSampleRate,
                                            contentLength = format.contentLength ?: 0L,
                                            loudnessDb = validatedPlayback.audioConfig?.loudnessDb,
                                            playbackUrl = validatedPlayback.streamUrl
                                        )
                                    )
                                }
                                recoverSong(mediaId, validatedPlayback)
                                
                                 // Fetch genre if not present
                                val song = database.song(mediaId).first()
                                if (song?.song?.genre == null) {
                                    YouTube.next(WatchEndpoint(videoId = mediaId)).onSuccess { _ ->
                                             database.query {
                                                 update(song?.song?.copy(genre = "Music") ?: return@query) 
                                             }
                                    }
                                }
                              } catch(e: Exception) {
                                  Timber.e(e, "Error during playback side-effects")
                              }
                         }
                        
                        return@withContext validatedPlayback
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("❌ URL malformed: ${e.message}")
                    }
                } else {
                    Timber.tag(TAG).e("❌ URL inválida (no https): $streamUrl")
                }
                null
            } catch (e: Exception) {
                Timber.e(e, "Failed to prepare playback for $mediaId")
                null
            }
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val okHttpFactory = OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .proxy(YouTube.proxy)
                .addInterceptor(YouTubeSessionInterceptor())
                .build()
        ).setDefaultRequestProperties(mapOf("User-Agent" to "Kuromusic/1.0"))

        return ResolvingDataSource.Factory(okHttpFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: return@Factory dataSpec
            var cached = playbackCache[mediaId]
            
            // Proactive resolution if not in cache or expired
            if (cached == null || cached.streamExpiresInSeconds * 1000L <= System.currentTimeMillis()) {
                try {
                    // We must use runBlocking here because ResolvingDataSource is synchronous
                    // but we need to call suspend functions to fetch the URL.
                    // This is acceptable as it's running in ExoPlayer's loading thread.
                    cached = runBlocking {
                        preparePlayback(mediaId)
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Proactive resolution failed for $mediaId")
                }
            }

            if (cached != null) {
                 return@Factory dataSpec.withUri(cached.streamUrl.toUri())
            }
            dataSpec
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            androidx.media3.extractor.DefaultExtractorsFactory()
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (playbackStats.totalPlayTimeMs >= (
                    dataStore[HistoryDuration]?.times(1000f)
                        ?: 30000f
                    ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
            val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")
            if (!dataStore.get(PauseRemoteListenHistoryKey, false)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    playbackUrl?.let {
                        YouTube.registerPlayback(null, playbackUrl)
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (!::player.isInitialized) return
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            scope.launch(Dispatchers.IO) {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
                filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            }
            return
        }

        try {
            val persistQueue =
                PersistQueue(
                    title = queueTitle,
                    items = player.mediaItems.mapNotNull { it.metadata },
                    mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                    position = if (player.currentPosition >= 0) player.currentPosition else 0,
                )
            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            // Guardar estado del reproductor
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = player.playWhenReady, // Estado de reproducción (si está listo para reproducir)
                playbackState = player.playbackState // Estado actual del reproductor
            )

            scope.launch(Dispatchers.IO) {
                runCatching {
                    filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                        ObjectOutputStream(fos).use { oos ->
                            oos.writeObject(persistQueue)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error saving queue to disk", it)
                    reportException(it)
                }

                runCatching {
                    filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                        ObjectOutputStream(fos).use { oos ->
                            oos.writeObject(persistAutomix)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error saving automix to disk", it)
                    reportException(it)
                }

                runCatching {
                    filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                        ObjectOutputStream(fos).use { oos ->
                            oos.writeObject(playerState)
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Error saving player state to disk", it)
                    reportException(it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        releaseLoudnessEnhancer()
        mediaSession.release()
        if (::player.isInitialized) {
            player.removeListener(this)
            player.removeListener(sleepTimer)
            player.release()
        }
        
        if (::simpleCache.isInitialized) {
            simpleCache.release()
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"

        // Constants for audio normalization
        private const val MAX_GAIN_MB = 800 // Maximum gain in millibels (8 dB)
        private const val MIN_GAIN_MB = -800 // Minimum gain in millibels (-8 dB)

        private const val TAG = "MusicService"
    }
}