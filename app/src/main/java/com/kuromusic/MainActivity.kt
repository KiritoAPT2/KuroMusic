package com.kuromusic

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.WatchEndpoint
import com.kuromusic.constants.AppBarHeight
import com.kuromusic.constants.DarkModeKey
import com.kuromusic.constants.DefaultOpenTabKey
import com.kuromusic.constants.DisableScreenshotKey
import com.kuromusic.constants.DynamicThemeKey
import com.kuromusic.constants.MiniPlayerHeight
import com.kuromusic.constants.NavigationBarAnimationSpec
import com.kuromusic.constants.NavigationBarHeight
import com.kuromusic.constants.PauseSearchHistoryKey
import com.kuromusic.constants.PlayerBackgroundStyle
import com.kuromusic.constants.PlayerBackgroundStyleKey
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.constants.SearchSource
import com.kuromusic.constants.SearchSourceKey
import com.kuromusic.constants.SlimNavBarKey
import com.kuromusic.constants.StopMusicOnTaskClearKey
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.SearchHistory
import com.kuromusic.extensions.toEnum
import com.kuromusic.models.toMediaMetadata
import com.kuromusic.playback.DownloadUtil
import com.kuromusic.playback.MusicService
import com.kuromusic.playback.MusicService.MusicBinder
import com.kuromusic.playback.PlayerConnection
import com.kuromusic.playback.queues.YouTubeQueue
import com.kuromusic.ui.component.AvatarPreferenceManager
import com.kuromusic.ui.component.AvatarSelection
import com.kuromusic.ui.component.BottomSheetMenu
import com.kuromusic.ui.component.IconButton
import com.kuromusic.ui.component.LocalMenuState
import com.kuromusic.ui.component.LocaleManager
import com.kuromusic.ui.component.Lyrics
import com.kuromusic.ui.component.SwitchPreference
import com.kuromusic.ui.component.TopSearch
import com.kuromusic.ui.component.rememberBottomSheetState
import com.kuromusic.ui.component.shimmer.ShimmerTheme
import com.kuromusic.ui.menu.YouTubeSongMenu
import com.kuromusic.ui.player.BottomSheetPlayer
import com.kuromusic.ui.player.MiniPlayer
import com.kuromusic.ui.screens.Screens
import com.kuromusic.ui.screens.navigationBuilder
import com.kuromusic.ui.screens.search.LocalSearchScreen
import com.kuromusic.ui.screens.search.OnlineSearchScreen
import com.kuromusic.ui.screens.settings.DarkMode
import com.kuromusic.ui.screens.settings.NavigationTab
import com.kuromusic.ui.theme.ColorSaver
import com.kuromusic.ui.theme.DefaultThemeColor
import com.kuromusic.ui.theme.KuroMusicTheme
import com.kuromusic.ui.theme.extractThemeColor
import com.kuromusic.ui.utils.appBarScrollBehavior
import com.kuromusic.ui.utils.backToMain
import com.kuromusic.ui.utils.resetHeightOffset
import com.kuromusic.utils.SyncUtils
// import com.kuromusic.utils.Updater
import com.kuromusic.utils.UpdateChecker
import com.kuromusic.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.kuromusic.utils.get
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference
import com.kuromusic.utils.reportException
import com.kuromusic.viewmodels.NewReleaseViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.FlowPreview
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kuromusic.workers.CacheCleanupWorker
import java.util.concurrent.TimeUnit



@OptIn(kotlinx.coroutines.FlowPreview::class)
@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        // Service binding moved to LaunchedEffect in onCreate to prevent cold start lag
    }

    override fun onStop() {
        super.onStop()
        // Liberamos la conexión para que no queden "fantasmas" 
        // escuchando cambios de estado en segundo plano.
        playerConnection?.dispose() 
        try {
            unbindService(serviceConnection)
        } catch (e: Exception) {
            // Service wasn't bound yet (e.g. app closed < 500ms), ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 101)
        }

        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        intent?.let { handlevideoIdIntent(it) }

        setContent {
            LaunchedEffect(Unit) {
                // Defer blocking service binding to allow UI to draw first frame (Vsync Fix)
                delay(500)
                startService(Intent(this@MainActivity, MusicService::class.java))
                bindService(
                    Intent(this@MainActivity, MusicService::class.java),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )

                // Schedule Cache Cleanup (Every 2 days)
                val cleanupWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(
                    2, TimeUnit.DAYS
                ).build()

                WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                    "cache_cleanup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupWork
                )

                // Removed Updater.getLatestVersionName call
            }

            var showUpdateDialog by remember { mutableStateOf<com.kuromusic.utils.UpdateInfo?>(null) }
            
            // New UpdateChecker logic
            LaunchedEffect(Unit) {
                val update = UpdateChecker(this@MainActivity).checkUpdates()
                if (update != null) {
                    showUpdateDialog = update
                }
            }

            if (showUpdateDialog != null) {
                val updateInfo = showUpdateDialog!!
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = null },
                    icon = { Icon(painterResource(R.drawable.update), null) },
                    title = { Text("Nueva versión: ${updateInfo.version}") }, // Using hardcoded or context strings if preferred
                    text = { Text("¿Descargar actualización ahora?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUpdateDialog = null
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo.apkUrl))
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        ) {
                            Text("Sí, Descargar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = null }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            var showFullscreenLyrics by remember { mutableStateOf(false) }


            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = false)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            val useBlackBackground = pureBlack && useDarkTheme
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            val animatedDynamicColor by animateColorAsState(
                targetValue = themeColor,
                animationSpec = tween(durationMillis = 1000),
                label = "dynamicColor"
            )

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    playerConnection.service.currentMediaMetadata
                        .debounce(200L)
                        .distinctUntilChanged { old, new -> old?.id == new?.id }
                        .collectLatest { song ->
                            try {
                                themeColor = if (song != null) {
                                    withContext(Dispatchers.IO) {
                                        val result = imageLoader.execute(
                                            ImageRequest.Builder(this@MainActivity)
                                                .data(song.thumbnailUrl)
                                                .size(128, 128)
                                                .allowHardware(false)
                                                .build()
                                        )
                                        (result.drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                                            withContext(Dispatchers.Default) {
                                                bitmap.extractThemeColor()
                                            }
                                        } ?: DefaultThemeColor
                                    }
                                } else {
                                    DefaultThemeColor
                                }
                            } catch (e: Exception) {
                                Timber.tag("DynamicTheme").w(e, "Error extracting theme color")
                                themeColor = DefaultThemeColor
                            }
                        }
                }
            }

            KuroMusicTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                enableDynamicTheme = enableDynamicTheme,
                themeColor = themeColor,
            ) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(if (pureBlack) Color.Black else if (useDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFF8F9FA)),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                    val bottomInsetDp = remember(systemBarsPadding) { systemBarsPadding.calculateBottomPadding() }


                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab) = rememberSaveable { mutableStateOf("home") }

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_EXPLORE -> NavigationTab.EXPLORE
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Explore.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val isNavBarScreen =
                        remember(navBackStackEntry, active) {
                            (navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) &&
                                    !active
                        }

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = MiniPlayerHeight + (if (isNavBarScreen) NavigationBarHeight else bottomInsetDp + 4.dp),
                            expandedBound = maxHeight,
                        )

                    val playerProgress = playerBottomSheetState.progress
                    val shouldShowNavigationBar = remember(isNavBarScreen, playerProgress) {
                        isNavBarScreen && playerProgress < 0.05f
                    }

                    val systemBars = WindowInsets.systemBars
                    val playerAwareWindowInsets =
                        remember(
                            bottomInsetDp,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed,
                            systemBars // Add as key since we use it
                        ) {
                            var bottom = if (shouldShowNavigationBar) NavigationBarHeight else bottomInsetDp
                            if (!playerBottomSheetState.isDismissed) bottom = playerBottomSheetState.collapsedBound
                            systemBars
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    val shouldShowTopBar =
                        remember(active, navBackStackEntry) {
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                        }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener =
                            Consumer<Intent> { intent ->
                                val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                                    ?.toUri() ?: return@Consumer
                                when (val path = uri.pathSegments.firstOrNull()) {
                                    "playlist" ->
                                        uri.getQueryParameter("list")?.let { playlistId ->
                                            if (playlistId.startsWith("OLAK5uy_")) {
                                                coroutineScope.launch {
                                                    YouTube
                                                        .albumSongs(playlistId)
                                                        .onSuccess { songs ->
                                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                navController.navigate("album/$browseId")
                                                            }
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                }
                                            } else {
                                                navController.navigate("online_playlist/$playlistId")
                                            }
                                        }

                                    "browse" ->
                                        uri.lastPathSegment?.let { browseId ->
                                            navController.navigate("album/$browseId")
                                        }

                                    "channel", "c" ->
                                        uri.lastPathSegment?.let { artistId ->
                                            navController.navigate("artist/$artistId")
                                        }

                                    else ->
                                        when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            uri.host == "youtu.be" -> path
                                            else -> null
                                        }?.let { videoId ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    YouTube.queue(listOf(videoId))
                                                }.onSuccess {
                                                    playerConnection?.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                            it.firstOrNull()?.toMediaMetadata()
                                                        )
                                                    )
                                                }.onFailure {
                                                    reportException(it)
                                                }
                                            }
                                        }
                                }
                            }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitle = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Explore.route -> R.string.explore
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }
                    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                    val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        Scaffold(
                            topBar = {
                                val playerBackground by rememberEnumPreference(
                                    key = PlayerBackgroundStyleKey,
                                    defaultValue = PlayerBackgroundStyle.DEFAULT
                                )

                                if (shouldShowTopBar && !active) {
                                    val isHome = navBackStackEntry?.destination?.route == Screens.Home.route
                                    val isLibrary = navBackStackEntry?.destination?.route == Screens.Library.route
                                    val isExplore = navBackStackEntry?.destination?.route == Screens.Explore.route

                                    val identityContentColor = if (useDarkTheme) Color.White else Color.Black

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(10f)
                                    ) {
                                        // Edge-to-edge immersive mode - no background box

                                        TopAppBar(
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 16.dp)
                                                ) {
                                                    if (!isHome && !isLibrary && !isExplore) {
                                                        Image(
                                                            painter = painterResource(R.drawable.drawable_ic_kuro),
                                                            contentDescription = null,
                                                            colorFilter = ColorFilter.tint(identityContentColor),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    } else if (isHome) {
                                                        Image(
                                                            painter = painterResource(R.drawable.drawable_ic_kuro),
                                                            contentDescription = null,
                                                            colorFilter = ColorFilter.tint(identityContentColor),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }

                                                    Text(
                                                        text = if (isHome) stringResource(R.string.app_name) else (currentTitle?.let { stringResource(it) } ?: stringResource(R.string.app_name)),
                                                        style = (if (isLibrary || isExplore) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge).copy(
                                                            fontSize = 22.sp,
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = (-0.5).sp
                                                        ),
                                                        color = identityContentColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },

                                            actions = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                ) {
                                                    val context = LocalContext.current

                                                    IconButton(
                                                        onClick = { onActiveChange(true) }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.search),
                                                            contentDescription = stringResource(R.string.search),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    ProfileIconWithUpdateBadge(
                                                        currentVersion = BuildConfig.VERSION_NAME,
                                                        onProfileClick = {
                                                            try {
                                                                navController.navigate("settings")
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                                Toast.makeText(
                                                                    context,
                                                                    R.string.navigation_error,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            scrollBehavior = if (isLibrary || isExplore) null else searchBarScrollBehavior,
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent,
                                                scrolledContainerColor = Color.Transparent
                                            )
                                        )
                                    }
                                }

                                // Verificación más segura para la ruta
                                val isSearchRoute =
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true

                                if (active || isSearchRoute) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.string.search_library
                                                        SearchSource.ONLINE -> R.string.search_yt_music
                                                    }
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> onActiveChange(false)
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.navigateUp()
                                                        }

                                                        else -> onActiveChange(true)
                                                    }
                                                },
                                                onLongClick = {
                                                    when {
                                                        active -> {}
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.backToMain()
                                                        }
                                                        else -> {}
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active ||
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                        ) {
                                                            R.drawable.arrow_back
                                                        } else {
                                                            R.drawable.search
                                                        },
                                                    ),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (active) {
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                onQueryChange(TextFieldValue(""))
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.close),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            searchSource =
                                                                if (searchSource == SearchSource.ONLINE) {
                                                                    SearchSource.LOCAL
                                                                } else {
                                                                    SearchSource.ONLINE
                                                                }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.drawable.library_music
                                                                    SearchSource.ONLINE -> R.drawable.language
                                                                }
                                                            ),
                                                            contentDescription = stringResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.string.search_online
                                                                    SearchSource.ONLINE -> R.string.search_library
                                                                }
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .focusRequester(searchBarFocusRequester)
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth(),
                                        focusRequester = searchBarFocusRequester
                                    ) {
                                        Crossfade(
                                            targetState = searchSource,
                                            label = "search_content_transition",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    bottom = if (!playerBottomSheetState.isDismissed) {
                                                        MiniPlayerHeight
                                                    } else {
                                                        0.dp
                                                    }
                                                )
                                                .navigationBarsPadding(),
                                        ) { currentSearchSource ->
                                            when (currentSearchSource) {
                                                SearchSource.LOCAL -> LocalSearchScreen(
                                                    query = query.text,
                                                    navController = navController,
                                                    onDismiss = { onActiveChange(false) },
                                                    pureBlack = pureBlack,
                                                )

                                                SearchSource.ONLINE -> OnlineSearchScreen(
                                                    query = query.text,
                                                    onQueryChange = onQueryChange,
                                                    navController = navController,
                                                    onSearch = { searchQuery ->
                                                        try {
                                                            val encodedQuery = URLEncoder.encode(
                                                                searchQuery,
                                                                "UTF-8"
                                                            )
                                                            navController.navigate("search/$encodedQuery")

                                                            // Verificar preferencias antes de guardar historial
                                                            if (dataStore[PauseSearchHistoryKey] != true) {
                                                                database.query {
                                                                    insert(SearchHistory(query = searchQuery))
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(
                                                                "SearchNavigation",
                                                                "Error navigating to search: ${e.message}",
                                                                e
                                                            )
                                                        }
                                                    },
                                                    onDismiss = { onActiveChange(false) },
                                                )
                                            }
                                        }
                                    }
                                }
                            },

                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                                .background(MaterialTheme.colorScheme.surface),
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            var transitionDirection =
                                AnimatedContentTransitionScope.SlideDirection.Left

                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                if (navigationItems.fastAny { it.route == previousTab }) {
                                    val curIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == navBackStackEntry?.destination?.route
                                        }
                                    )

                                    val prevIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == previousTab
                                        }
                                    )

                                    if (prevIndex > curIndex)
                                        AnimatedContentTransitionScope.SlideDirection.Right.also {
                                            transitionDirection = it
                                        }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                    NavigationTab.HOME -> Screens.Home
                                    NavigationTab.EXPLORE -> Screens.Explore
                                    NavigationTab.LIBRARY -> Screens.Library
                                }.route,

                                enterTransition = {
                                    if (initialState.destination.route in topLevelScreens &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                                slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                                )
                                    }
                                },

                                exitTransition = {
                                    if (initialState.destination.route in topLevelScreens &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                                slideOutHorizontally(
                                                    targetOffsetX = { -it / 5 },
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                    }
                                },

                                popEnterTransition = {
                                    if ((initialState.destination.route in topLevelScreens ||
                                                initialState.destination.route?.startsWith("search/") == true) &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                                slideInHorizontally(
                                                    initialOffsetX = { -it },
                                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                                )
                                    }
                                },

                                popExitTransition = {
                                    if ((initialState.destination.route in topLevelScreens ||
                                                initialState.destination.route?.startsWith("search/") == true) &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                                slideOutHorizontally(
                                                    targetOffsetX = { it / 5 },
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                    }
                                },

                                modifier = Modifier.nestedScroll(
                                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                    ) {
                                        searchBarScrollBehavior.nestedScrollConnection
                                    } else {
                                        topAppBarScrollBehavior.nestedScrollConnection
                                    }
                                )
                            ) {
                                navigationBuilder(
                                    navController,
                                    topAppBarScrollBehavior,
                                    latestVersionName
                                )
                            }

                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                        }
                    }

                        // BottomSheetPlayer - DEBE SER EL ÚLTIMO ELEMENTO DEL BoxWithConstraints
                        if (navBackStackEntry?.destination?.route != "lyrics") {
                            BottomSheetPlayer(
                                state = playerBottomSheetState,
                                navController = navController,
                                onOpenFullscreenLyrics = {
                                    playerBottomSheetState.collapseSoft()
                                    navController.navigate("lyrics")
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(15f),
                                dynamicColor = animatedDynamicColor
                            )
                        }

                        // DARK GRADIENT PROTECTION for NavigationBar (Premium bottom scrim - Visually tied to NavBar)
                        AnimatedVisibility(
                            visible = shouldShowNavigationBar,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(90f),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Black.copy(alpha = 0.5f),
                                                Color.Black.copy(alpha = 0.8f),
                                                Color.Black.copy(alpha = 0.95f) // Slightly reduced from solid
                                            )
                                        )
                                    )
                            )
                        }

                        // NAVBAR - Transparent with white icons
                        AnimatedVisibility(
                            visible = shouldShowNavigationBar,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(100f),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            // NavigationBar transparent - icons always white for visibility
                            // NavigationBar transparent container
                            NavigationBar(
                                modifier = Modifier
                                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(50.dp)),
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToScreen(navController, screen) },
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        if (selected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = null
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(screen.titleId),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            alwaysShowLabel = false,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = if (enableDynamicTheme) animatedDynamicColor else Color.White,
                                                selectedTextColor = if (enableDynamicTheme) animatedDynamicColor else Color.White,
                                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                                indicatorColor = if (enableDynamicTheme) animatedDynamicColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToScreen(
        navController: NavHostController,
        screen: Screens
    ) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.queue(listOf(videoId))
                }.onSuccess {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.kuromusic.action.SEARCH"
        const val ACTION_EXPLORE = "com.kuromusic.action.EXPLORE"
        const val ACTION_LIBRARY = "com.kuromusic.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    // Función para verificar permisos extraída para mejor legibilidad
    val checkNotificationPermission = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED // Usar PackageManager en lugar de PermissionChecker
            } else {
                // Para versiones anteriores, verificar si las notificaciones están habilitadas
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        // Opcional: agregar callback para manejar el resultado
        if (!isGranted) {
            // Manejar caso cuando el usuario rechaza el permiso
            Log.d("NotificationPermission", "Permiso de notificaciones denegado")
        }
    }

    // Verificar permisos al inicializar y cuando la app vuelve al foreground
    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    // Escuchar cambios cuando la app vuelve del background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(
                    id = if (permissionGranted) R.drawable.notification_on
                    else R.drawable.notification_off
                ),
                contentDescription = stringResource(
                    if (permissionGranted) R.string.notifications_enabled
                    else R.string.notifications_disabled
                )
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            when {
                checked && !permissionGranted -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Para versiones anteriores, dirigir a configuración
                        openNotificationSettings(context)
                    }
                }

                !checked && permissionGranted -> {
                    // Si el usuario intenta desactivar, dirigir a configuración del sistema
                    openNotificationSettings(context)
                }
            }
        }
    )
}


// Función auxiliar para abrir configuración de notificaciones
private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("NotificationSettings", "No se pudo abrir configuración de notificaciones", e)
        // Fallback: abrir configuración general
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/KuroMusic/KuroMusic/releases/latest")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    // Animación del badge
    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Control seguro de updates
    LaunchedEffect(currentVersion) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateBadge = latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (e: Exception) {
            Timber.tag("ProfileIcon").e("Error checking for updates: ${e.message}")
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try {
                    updatedOnClick.value()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    ) {
        // Avatar usando el nuevo sistema
        Box(contentAlignment = Alignment.Center) {
            when (currentSelection) {
                is AvatarSelection.Custom -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = "Avatar personalizado",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                is AvatarSelection.DiceBear -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data((currentSelection as AvatarSelection.DiceBear).url)
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = "Avatar DiceBear",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Avatar predeterminado",
                        modifier = modifier
                    )
                }
            }
        }

        // Badge de actualización mejorado - dentro del avatar
        if (showUpdateBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            ) {
                // Anillo de pulso exterior
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Overlay semi-transparente
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                )

                // Ícono de actualización con animación
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = "Actualización disponible",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                        .scale(scale)
                        .alpha(alpha)
                )
            }
        }
    }
}
