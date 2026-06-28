package com.kuromusic.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.offline.Download

import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuromusic.LocalDatabase
import com.kuromusic.LocalDownloadUtil
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.constants.DarkModeKey
import com.kuromusic.constants.DefaultPlayPauseButtonShape
import com.kuromusic.constants.DefaultSmallButtonsShape
import com.kuromusic.constants.PlayPauseButtonShapeKey
import com.kuromusic.constants.PlayerBackgroundStyle
import com.kuromusic.constants.PlayerBackgroundStyleKey
import com.kuromusic.constants.PlayerButtonsStyle
import com.kuromusic.constants.PlayerButtonsStyleKey
import com.kuromusic.constants.PlayerHorizontalPadding
import com.kuromusic.constants.PlayerTextAlignmentKey
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.constants.QueuePeekHeight
import com.kuromusic.constants.ShowLyricsKey
import com.kuromusic.constants.SliderStyle
import com.kuromusic.constants.SliderStyleKey
import com.kuromusic.constants.SmallButtonsShapeKey
import com.kuromusic.constants.BeatBuddyType
import com.kuromusic.constants.BeatBuddyTypeKey
import me.saket.squiggles.SquigglySlider
import com.kuromusic.extensions.togglePlayPause
import com.kuromusic.extensions.toggleRepeatMode
import com.kuromusic.models.MediaMetadata
import com.kuromusic.ui.component.BottomSheet
import com.kuromusic.ui.component.BottomSheetState
import com.kuromusic.ui.component.LocalMenuState
import com.kuromusic.ui.component.PlayerSliderTrack
import com.kuromusic.ui.component.ResizableIconButton
import com.kuromusic.ui.component.rememberBottomSheetState
import com.kuromusic.ui.menu.PlayerMenu
import com.kuromusic.ui.screens.settings.DarkMode
import com.kuromusic.ui.screens.settings.PlayerTextAlignment
import com.kuromusic.ui.theme.PlayerColorExtractor
import com.kuromusic.ui.theme.PlayerSliderColors
import com.kuromusic.ui.theme.extractGradientColors
import com.kuromusic.utils.getPlayPauseShape
import com.kuromusic.utils.getSmallButtonShape
import com.kuromusic.utils.makeTimeString
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    dynamicColor: Color? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current

    val clipboardManager = context.getSystemService(ClipboardManager::class.java)!!

    var showFullscreenLyrics by remember { mutableStateOf(false) }
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)
    val beatBuddyType by rememberEnumPreference(BeatBuddyTypeKey, BeatBuddyType.CAT)

    // State Hoisting: Use '=' to keep the State object, do NOT read .longValue here!
    // NOTE: No key — positionState must be a single stable object so both
    // LaunchedEffects (mediaMetadata and playbackState) always mutate the same
    // MutableLongState. Using playbackState as key would re-create it on every
    // state transition (3→2→3 during skip), causing stale reference bugs.
    val positionState = rememberSaveable {
        mutableLongStateOf(0L)
    }
    val durationState = remember {
        mutableLongStateOf(-1L)
    }
    // Observe _duration StateFlow from PlayerConnection (updated via onTimelineChanged)
    LaunchedEffect(Unit) {
        playerConnection.duration.collect { duration ->
            durationState.longValue = duration
        }
    }

    val sliderProgress by remember {
        derivedStateOf {
            if (durationState.longValue > 0) {
                positionState.longValue.toFloat() / durationState.longValue
            } else 0f
        }
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }



    // Animations for background effects
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    // Obtener el color del tema antes de LaunchedEffect
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColorArgb = surfaceColor.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground, fallbackColorArgb) {
        // Update image URL for smooth transitions
        backgroundImageUrl = mediaMetadata?.thumbnailUrl

        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    ImageLoader(context)
                        .execute(
                            ImageRequest
                                .Builder(context)
                                .data(mediaMetadata?.thumbnailUrl)
                                .allowHardware(false)
                                .build(),
                        ).drawable as? BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { bitmap ->
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColorArgb
                    )

                    withContext(Dispatchers.Main) {
                        // Create a more dramatic gradient for premium look
                        gradientColors = if (extractedColors.isNotEmpty()) {
                            listOf(extractedColors[0], Color.Black)
                        } else {
                            extractedColors
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val onBackgroundColor = remember(playerBackground, gradientColors, useDarkTheme) {
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> Color.Unspecified 
            else -> {
                if (gradientColors.isNotEmpty()) {
                    val luminance = ColorUtils.calculateLuminance(gradientColors[0].toArgb())
                    if (luminance > 0.5) Color.Black else Color.White
                } else {
                    if (useDarkTheme) Color.White else Color.Black
                }
            }
        }
    }
    
    // Resolve the actual color (handling Composable access for Default)
    val finalOnBackgroundColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
        MaterialTheme.colorScheme.secondary
    } else {
        onBackgroundColor
    }

    val icBackgroundColor = remember(playerBackground, finalOnBackgroundColor) {
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> Color.Unspecified // Placeholder
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> finalOnBackgroundColor
        }
    }
    val finalIcBackgroundColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.surface else icBackgroundColor

    val TextBackgroundColor = remember(playerBackground, gradientColors) {
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> Color.Unspecified
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    Color.White
                } else {
                    Color.White
                }
            }
        }
    }
    val finalTextBackgroundColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onBackground else TextBackgroundColor
    
    // Re-assign to original names to minimize downstream changes
    val resolvedOnBackgroundColor = finalOnBackgroundColor
    val resolvedIcBackgroundColor = finalIcBackgroundColor
    val resolvedTextBackgroundColor = finalTextBackgroundColor

    // Sistema de color scheme con PRIMARY y TERTIARY colors
    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT ->
            if (useDarkTheme) Pair(Color.White, Color.Black)
            else Pair(Color.Black, Color.White)
        PlayerButtonsStyle.PRIMARY -> Pair(
            resolvedOnBackgroundColor,
            resolvedIcBackgroundColor
        )
        PlayerButtonsStyle.TERTIARY -> Pair(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
    }

    val downloadUtil = LocalDownloadUtil.current
    val download by downloadUtil.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    // SleepTimerDialog extracted
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            playerConnection = playerConnection
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val smallButtonShape = remember(smallButtonsShapeState.value) {
        getSmallButtonShape(smallButtonsShapeState.value)
    }

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val playPauseShape = remember(playPauseShapeState.value) {
        getPlayPauseShape(playPauseShapeState.value)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "play_pause_rotation")
    val playPauseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000, // 9 seconds for a full rotation
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Forma dinámica: cuando está reproduciendo usa la forma seleccionada
    // Cuando está en pausa usa Square
    val currentPlayPauseShape = remember(isPlaying, playPauseShape) {
        if (isPlaying) {
            playPauseShape
        } else {
            MaterialShapes.Square
        }
    }

    // Function to create the modifier for small buttons
    val smallButtonModifier = @Composable {
        Modifier
            .size(42.dp)
            .clip(smallButtonShape.toShape())
            .background(textButtonColor)
    }

    LaunchedEffect(mediaMetadata) {
        // Reset to 0 when a new track starts — player.currentPosition during
        // transition may still report the previous track's position.
        positionState.longValue = 0L
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                positionState.longValue = playerConnection.player.currentPosition
            }
        }
    }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        SongDetailsDialog(
            onDismiss = { showDetailsDialog = false },
            mediaMetadata = mediaMetadata,
            playerConnection = playerConnection,
            useBlackBackground = useBlackBackground,
            context = context,
            clipboardManager = clipboardManager
        )
    }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
                .calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    val bottomSheetBackgroundColor = when {
        useBlackBackground -> Color.Black
        playerBackground == PlayerBackgroundStyle.BLUR || playerBackground == PlayerBackgroundStyle.GRADIENT ->
            MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    SharedTransitionLayout {
        val sharedTransitionScope = this
        BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                 if (useBlackBackground) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LocalConfiguration.current.screenHeightDp.dp * 0.35f)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.Black
                                    )
                                )
                            )
                    )
                }

                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(200) // Lower res for blur (Cold Start fix)
                                            .allowHardware(true) // Enable hardware acceleration
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(if (useDarkTheme) 150.dp else 100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                if (useBlackBackground) {
                                    // AMOLED MODE: Radial glow behind cover
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(backgroundAlpha)
                                            .background(Color.Black)
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(backgroundAlpha)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        colors[0].copy(alpha = 0.25f),
                                                        Color.Black
                                                    ),
                                                    center = Offset.Unspecified, 
                                                    radius = 1500f
                                                )
                                            )
                                    )
                                } else {
                                    // STANDARD DARK MODE: Vertical premium gradient
                                    val topColor = colors[0].copy(alpha = 1f).let { 
                                        Color(
                                            red = it.red * 0.6f,
                                            green = it.green * 0.6f,
                                            blue = it.blue * 0.6f,
                                            alpha = 1f
                                        )
                                    }
                                    val bottomColor = Color(0xFF121212)
                                    
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(backgroundAlpha)
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to topColor,
                                                    1f to bottomColor
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            AnimatedVisibility(
                visible = !state.isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MiniPlayerWrapper(
                    positionState = positionState,
                    durationState = durationState,
                    dynamicColor = dynamicColor,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this
                )
            }
        },
    ) {
        // Deferred composition for Cold Start fix
        if (state.progress > 0.01f) {
            val onPlayPauseRemembered = remember(playerConnection) {
                {
                    if (playerConnection.player.playbackState == STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                }
            }
            val onSkipNextRemembered = remember(playerConnection) { { playerConnection.seekToNext() } }
            val onSkipPreviousRemembered = remember(playerConnection) { { playerConnection.seekToPrevious() } }
            val onToggleLikeRemembered = remember(playerConnection) { { playerConnection.toggleLike() } }
            val onMoreClickRemembered = remember(menuState, mediaMetadata) {
                {
                    if (mediaMetadata != null) {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata!!,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = { showDetailsDialog = true },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                    Unit
                }
            }
            val onTitleClickRemembered = remember(mediaMetadata, navController, state) {
                {
                    if (mediaMetadata?.album != null) {
                        navController.navigate("album/${mediaMetadata!!.album!!.id}")
                        state.collapseSoft()
                    }
                    Unit
                }
            }
            val onTitleLongClickRemembered = remember(mediaMetadata, clipboardManager, context) {
                {
                    mediaMetadata?.title?.let { title ->
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, title))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                    Unit
                }
            }
            val onArtistClickRemembered = remember(navController, state) {
                { artistId: String? ->
                    artistId?.let {
                        navController.navigate("artist/$it")
                        state.collapseSoft()
                    }
                    Unit
                }
            }
            val onDownloadClickRemembered = remember(download?.state, mediaMetadata, database) {
                {
                    mediaMetadata?.let { metadata ->
                        when (download?.state) {
                            Download.STATE_COMPLETED,
                            Download.STATE_QUEUED,
                            Download.STATE_DOWNLOADING -> {
                                downloadUtil.removeDownload(metadata.id)
                            }
                            else -> {
                                database.transaction { insert(metadata) }
                                downloadUtil.startDownload(metadata.id, metadata.title)
                            }
                        }
                    }
                    Unit
                }
            }




        // Animated background effects
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background with blurred image
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.BLUR && backgroundImageUrl != null,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(400))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backgroundImageUrl)
                        .size(200)
                        .allowHardware(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius)
                        .alpha(backgroundAlpha)
                )
            }

            // Animated gradient background
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2,
                enter = fadeIn(tween(800)),
                exit = fadeOut(tween(600))
            ) {
                val animatedGradientColors = gradientColors.map { color ->
                    androidx.compose.animation.animateColorAsState(
                        targetValue = color,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "gradientColor"
                    ).value
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backgroundAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = if (animatedGradientColors.isNotEmpty()) animatedGradientColors else gradientColors
                            )
                        )
                )
            }

            // Animated dark overlay
            AnimatedVisibility(
                visible = overlayAlpha > 0f,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }

            // Additional overlay for lyrics
            if (playerBackground != PlayerBackgroundStyle.DEFAULT && showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = animateFloatAsState(
                                    targetValue = if (state.isExpanded) 0.4f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = "lyricsOverlay"
                                ).value
                            )
                        )
                )
            }
        }
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(top = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp
                        val thumbnailSize = (screenWidth * 0.4).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedVisibility(
                                visible = state.isExpanded,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Thumbnail(
                                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                    modifier = Modifier.size(thumbnailSize),
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = this
                                )
                            }
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            // Pass remembered lambdas here (Same as portrait)
                            PlayerControls(
                                mediaMetadata = it,
                                playerConnection = playerConnection,
                                positionState = positionState,
                                durationState = durationState,
                                resolvedOnBackgroundColor = resolvedOnBackgroundColor,
                                iconButtonColor = iconButtonColor,
                                sliderStyle = sliderStyle,
                                downloadState = download,
                                isPlaying = isPlaying,
                                canSkipPrevious = canSkipPrevious,
                                canSkipNext = canSkipNext,
                                playbackState = playbackState,
                                currentSongLiked = currentSong?.song?.liked == true,
                                beatBuddyType = beatBuddyType,
                                sleepTimerEnabled = sleepTimerEnabled,
                                onTitleClick = onTitleClickRemembered,
                                onTitleLongClick = onTitleLongClickRemembered,
                                onArtistClick = { id -> onArtistClickRemembered(id) },
                                onToggleLike = onToggleLikeRemembered,
                                onDownloadClick = onDownloadClickRemembered,
                                onMoreClick = onMoreClickRemembered,
                                onSleepTimerClick = { showSleepTimerDialog = true },
                                onPlayPause = onPlayPauseRemembered,
                                onSkipPrevious = onSkipPreviousRemembered,
                                onSkipNext = onSkipNextRemembered
                            )
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                        ) {
                            AnimatedVisibility(
                                visible = state.isExpanded,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Thumbnail(
                                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = this
                                )
                            }
                        }
                    }

                    mediaMetadata?.let {
                        // Pass remembered lambdas here
                        PlayerControls(
                            mediaMetadata = it,
                            playerConnection = playerConnection,
                            positionState = positionState,
                            durationState = durationState,
                            resolvedOnBackgroundColor = resolvedOnBackgroundColor,
                            iconButtonColor = iconButtonColor,
                            sliderStyle = sliderStyle,
                            downloadState = download,
                            isPlaying = isPlaying,
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            playbackState = playbackState,
                            currentSongLiked = currentSong?.song?.liked == true,
                            beatBuddyType = beatBuddyType,
                            sleepTimerEnabled = sleepTimerEnabled,
                            onTitleClick = onTitleClickRemembered,
                            onTitleLongClick = onTitleLongClickRemembered,
                            onArtistClick = { id -> onArtistClickRemembered(id) },
                            onToggleLike = onToggleLikeRemembered,
                            onDownloadClick = onDownloadClickRemembered,
                            onMoreClick = onMoreClickRemembered,
                            onSleepTimerClick = { showSleepTimerDialog = true },
                            onPlayPause = onPlayPauseRemembered,
                            onSkipPrevious = onSkipPreviousRemembered,
                            onSkipNext = onSkipNextRemembered
                        )
                    }

                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = resolvedOnBackgroundColor,
            textBackgroundColor = resolvedTextBackgroundColor,
            )
        }
    } // End of Deferred Composition if
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    playerConnection: com.kuromusic.playback.PlayerConnection
) {
    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.bedtime),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.sleep_timer)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pluralStringResource(
                        R.plurals.minute,
                        sleepTimerValue.roundToInt(),
                        sleepTimerValue.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Slider(
                    value = sleepTimerValue,
                    onValueChange = { sleepTimerValue = it },
                    valueRange = 5f..120f,
                    steps = (120 - 5) / 5 - 1,
                )

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        playerConnection.service.sleepTimer.start(-1)
                    },
                ) {
                    Text(stringResource(R.string.end_of_song))
                }
            }
        },
    )
}

@Composable
private fun SongDetailsDialog(
    onDismiss: () -> Unit,
    mediaMetadata: MediaMetadata?,
    playerConnection: com.kuromusic.playback.PlayerConnection,
    useBlackBackground: Boolean,
    context: android.content.Context,
    clipboardManager: ClipboardManager
) {
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    
    AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = onDismiss,
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.MiniPlayerWrapper(
    positionState: androidx.compose.runtime.MutableLongState,
    durationState: androidx.compose.runtime.MutableLongState,
    dynamicColor: Color?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    MiniPlayer(
        position = positionState.longValue,
        duration = durationState.longValue,
        dynamicColor = dynamicColor,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    playerConnection: com.kuromusic.playback.PlayerConnection,
    positionState: androidx.compose.runtime.MutableLongState,
    durationState: androidx.compose.runtime.MutableLongState,
    onBackgroundColor: Color,
    sliderStyle: SliderStyle,
    modifier: Modifier = Modifier
) {
    // Internal slider position for smooth dragging
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    val currentPositionText by remember {
        derivedStateOf { makeTimeString(sliderPosition ?: positionState.longValue) }
    }
    val totalDurationText by remember {
        derivedStateOf { makeTimeString(durationState.longValue) }
    }

    val progress by remember {
        derivedStateOf {
            if (durationState.longValue > 0) {
                (sliderPosition ?: positionState.longValue).toFloat() / durationState.longValue
            } else 0f
        }
    }

    Column(modifier = modifier) {
        val sliderInteractionSource = remember { MutableInteractionSource() }
        val isSliderPressed by sliderInteractionSource.collectIsPressedAsState()
        
        when (sliderStyle) {
            SliderStyle.DEFAULT -> {
                Slider(
                    value = progress,
                    valueRange = 0f..1f,
                    onValueChange = {
                        sliderPosition = (it * durationState.longValue).toLong()
                    },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            playerConnection.player.seekTo(it)
                            positionState.longValue = it
                        }
                        sliderPosition = null
                    },
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            colors = SliderDefaults.colors(thumbColor = onBackgroundColor)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = onBackgroundColor,
                                inactiveTrackColor = onBackgroundColor.copy(alpha = 0.2f)
                            ),
                            thumbTrackGapSize = 0.dp
                        )
                    }
                )
            }
            SliderStyle.SQUIGGLY -> {
                val isPlaying by playerConnection.isPlaying.collectAsState()
                SquigglySlider(
                    value = progress,
                    valueRange = 0f..1f,
                    onValueChange = {
                        sliderPosition = (it * durationState.longValue).toLong()
                    },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            playerConnection.player.seekTo(it)
                            positionState.longValue = it
                        }
                        sliderPosition = null
                    },
                    interactionSource = sliderInteractionSource,
                    colors = SliderDefaults.colors(
                        activeTrackColor = onBackgroundColor,
                        inactiveTrackColor = onBackgroundColor.copy(alpha = 0.2f),
                        thumbColor = onBackgroundColor
                    ),
                    squigglesSpec = SquigglySlider.SquigglesSpec(
                        amplitude = if (isPlaying && !isSliderPressed) 3.dp else 0.dp,
                        strokeWidth = 4.dp
                    )
                )
            }
            SliderStyle.SLIM -> {
                Slider(
                    value = progress,
                    valueRange = 0f..1f,
                    onValueChange = {
                        sliderPosition = (it * durationState.longValue).toLong()
                    },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            playerConnection.player.seekTo(it)
                            positionState.longValue = it
                        }
                        sliderPosition = null
                    },
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        if (isSliderPressed) {
                            SliderDefaults.Thumb(
                                interactionSource = sliderInteractionSource,
                                colors = SliderDefaults.colors(thumbColor = onBackgroundColor)
                            )
                        }
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(2.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = onBackgroundColor,
                                inactiveTrackColor = onBackgroundColor.copy(alpha = 0.2f)
                            ),
                            thumbTrackGapSize = 0.dp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Tiempos
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
        ) {
            Text(
                text = currentPositionText,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = totalDurationText,
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    mediaMetadata: MediaMetadata,
    playerConnection: com.kuromusic.playback.PlayerConnection,
    positionState: androidx.compose.runtime.MutableLongState,
    durationState: androidx.compose.runtime.MutableLongState,
    resolvedOnBackgroundColor: Color,
    iconButtonColor: Color,
    sliderStyle: SliderStyle,
    downloadState: Download?,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playbackState: Int,
    currentSongLiked: Boolean,
    beatBuddyType: BeatBuddyType,
    sleepTimerEnabled: Boolean,
    onTitleClick: () -> Unit,
    onTitleLongClick: () -> Unit,
    onArtistClick: (String?) -> Unit,
    onToggleLike: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    // --- Título y Artistas (Debajo de la carátula) ---
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "titleAnimation",
            ) { title ->
                DisableSelection {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = resolvedOnBackgroundColor,
                        modifier = Modifier
                            .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                            .combinedClickable(
                                enabled = true,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onTitleClick,
                                onLongClick = onTitleLongClick
                            ),
                    )
                }
            }

            // Artistas con navegación
            if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                ) {
                    DisableSelection {
                        mediaMetadata.artists.forEachIndexed { index, artist ->
                            Text(
                                text = artist.name.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    letterSpacing = 1.sp
                                ),
                                color = resolvedOnBackgroundColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clickable(enabled = artist.id != null) {
                                        onArtistClick(artist.id)
                                    }
                            )
                            if (index != mediaMetadata.artists.lastIndex) {
                                Text(
                                    text = " / ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        letterSpacing = 1.sp,
                                        color = resolvedOnBackgroundColor.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // BOTONES DE ACCIÓN (Like, Download, More — horizontal)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Like
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint = if (currentSongLiked) MaterialTheme.colorScheme.error else resolvedOnBackgroundColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Botón Download
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(28.dp)
            ) {
                when (downloadState?.state) {
                    Download.STATE_COMPLETED -> Icon(painter = painterResource(R.drawable.offline), contentDescription = null, tint = resolvedOnBackgroundColor, modifier = Modifier.size(28.dp))
                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = resolvedOnBackgroundColor)
                    else -> Icon(painter = painterResource(R.drawable.download), contentDescription = null, tint = resolvedOnBackgroundColor, modifier = Modifier.size(28.dp))
                }
            }

            // Botón Sleep Timer
            Box {
                IconButton(
                    onClick = onSleepTimerClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        tint = if (sleepTimerEnabled) MaterialTheme.colorScheme.primary else resolvedOnBackgroundColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (sleepTimerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            // Botón More Options
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = resolvedOnBackgroundColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Separador de diseño
    Box(
        modifier = Modifier
            .padding(horizontal = PlayerHorizontalPadding + 8.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(resolvedOnBackgroundColor.copy(alpha = 0.08f))
    )

    Spacer(Modifier.height(16.dp))

    // Slider Estilizado

    // PlayerSlider extracted (Using State objects)
    PlayerSlider(
        playerConnection = playerConnection,
        positionState = positionState,
        durationState = durationState,
        onBackgroundColor = resolvedOnBackgroundColor,
        sliderStyle = sliderStyle,
        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
    )

    Spacer(Modifier.height(8.dp))

    // Beat Buddy
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        BeatBuddy(
            type = beatBuddyType,
            isPlaying = isPlaying,
            color = resolvedOnBackgroundColor,
        )
    }

    Spacer(Modifier.height(4.dp))

    // CONTROLES DE REPRODUCCIÓN CON ANIMACIONES AL PRESIONAR
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        val backInteractionSource = remember { MutableInteractionSource() }
        val nextInteractionSource = remember { MutableInteractionSource() }
        val playPauseInteractionSource = remember { MutableInteractionSource() }
        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        val isNextPressed by nextInteractionSource.collectIsPressedAsState()

        // Pesos animados con spring animation
        val playPauseWeight by animateFloatAsState(
            targetValue = when {
                isPlayPausePressed -> 1.9f
                isBackPressed || isNextPressed -> 1.1f
                else -> 1.3f
            },
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "playPauseWeight"
        )
        val backButtonWeight by animateFloatAsState(
            targetValue = when {
                isBackPressed -> 0.65f
                isPlayPausePressed -> 0.35f
                else -> 0.45f
            },
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "backButtonWeight"
        )
        val nextButtonWeight by animateFloatAsState(
            targetValue = when {
                isNextPressed -> 0.65f
                isPlayPausePressed -> 0.35f
                else -> 0.45f
            },
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "nextButtonWeight"
        )

        // Colores de botones laterales (Más discretos y sin bordes pesados)
        val sideButtonContentColor = resolvedOnBackgroundColor.copy(alpha = 0.8f)

        // Botón Previous
        IconButton(
            onClick = onSkipPrevious,
            enabled = canSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = null,
                tint = sideButtonContentColor.copy(
                    alpha = if (canSkipPrevious) 1f else 0.3f
                ),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Play button pulse when playing
        val playButtonScale by animateFloatAsState(
            targetValue = if (isPlayPausePressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
            label = "playButtonScale"
        )

        val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Botón Play/Pause (Círculo grande, limpio y sin texto)
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(if (isPlaying && !isPlayPausePressed) pulseAnim else playButtonScale)
                .shadow(8.dp, CircleShape, spotColor = resolvedOnBackgroundColor.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(resolvedOnBackgroundColor)
                .clickable(
                    indication = null,
                    interactionSource = playPauseInteractionSource,
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.key(isPlaying) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = null,
                    tint = iconButtonColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Botón Next
        IconButton(
            onClick = onSkipNext,
            enabled = canSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = null,
                tint = sideButtonContentColor.copy(
                    alpha = if (canSkipNext) 1f else 0.3f
                ),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}