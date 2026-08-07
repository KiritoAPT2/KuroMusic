package com.kuromusic.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.constants.DarkModeKey
import com.kuromusic.constants.DefaultMiniPlayerThumbnailShape
import com.kuromusic.constants.MiniPlayerThumbnailShapeKey
import com.kuromusic.constants.MiniplayerEdgeGlowKey
import com.kuromusic.constants.MiniplayerPartyModeKey
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.constants.ThumbnailCornerRadius
import com.kuromusic.extensions.togglePlayPause
import com.kuromusic.models.MediaMetadata
import com.kuromusic.ui.icons.BrokenIcon
import com.kuromusic.ui.icons.BrokenIcons
import com.kuromusic.ui.screens.settings.DarkMode
import com.kuromusic.utils.getMiniPlayerThumbnailShape
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    dynamicColor: Color? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isNetworkConnected by playerConnection.isNetworkConnected.collectAsState()

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val edgeGlowEnabled by rememberPreference(MiniplayerEdgeGlowKey, false)
    val partyModeEnabled by rememberPreference(MiniplayerPartyModeKey, false)

    val edgeGlowAlpha by rememberInfiniteTransition(label = "edgeGlowAlpha").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val partyPhase by rememberInfiniteTransition(label = "partyPhase").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "partyPhase"
    )

    val edgeBorderWidth by animateDpAsState(
        targetValue = if ((edgeGlowEnabled || partyModeEnabled) && isPlaying) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "edgeBorderWidth"
    )

    val miniPlayerThumbnailShapeState = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val miniPlayerThumbnailShape = remember(miniPlayerThumbnailShapeState.value, isPlaying) {
        if (isPlaying) {
            getMiniPlayerThumbnailShape(miniPlayerThumbnailShapeState.value)
        } else {
            MaterialShapes.Circle
        }
    }

    val currentView = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.0f else 0.4f,
        label = "overlay_alpha",
        animationSpec = animationSpec
    )

    val currentThumbnailShape = remember(isPlaying, miniPlayerThumbnailShape) {
        if (isPlaying) {
            miniPlayerThumbnailShape
        } else {
            MaterialShapes.Square
        }
    }.toShape()

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(0.73f)

    val accentColor = dynamicColor ?: MaterialTheme.colorScheme.primary
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    // Colors for gradient overlays (Namida-style dynamic tint)
    val bgGradientTop = remember(accentColor) {
        accentColor.copy(alpha = 0.08f)
    }
    val bgGradientBottom = remember(accentColor) {
        accentColor.copy(alpha = 0.02f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.Transparent)
    ) {
        Surface(
            modifier = Modifier
                .then(
                    if (isTabletLandscape) {
                        Modifier
                            .width(480.dp)
                            .align(Alignment.CenterEnd)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(52.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                ),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent
        ) {
            // Base background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
            )

            // Dynamic color gradient overlay (Namida-style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(bgGradientTop, bgGradientBottom)
                        )
                    )
            )

            // Party mode / edge glow
            if (isPlaying && (edgeGlowEnabled || partyModeEnabled)) {
                val glowColor = if (partyModeEnabled) {
                    val hue = (partyPhase / 360f) * 360f
                    Color.hsl(hue, 0.8f, 0.6f)
                } else {
                    accentColor
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = edgeBorderWidth,
                            brush = Brush.linearGradient(
                                colors = if (partyModeEnabled) {
                                    val hue1 = ((partyPhase + 0f) / 360f * 360f) % 360f
                                    val hue2 = ((partyPhase + 120f) / 360f * 360f) % 360f
                                    val hue3 = ((partyPhase + 240f) / 360f * 360f) % 360f
                                    listOf(
                                        Color.hsl(hue1, 0.8f, 0.6f),
                                        Color.hsl(hue2, 0.8f, 0.6f),
                                        Color.hsl(hue3, 0.8f, 0.6f),
                                    )
                                } else {
                                    listOf(
                                        glowColor.copy(alpha = edgeGlowAlpha),
                                        glowColor.copy(alpha = edgeGlowAlpha * 0.5f),
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += adjustedDragAmount.absoluteValue
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity =
                                    if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (0.73f * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                        currentOffset.absoluteValue > minDistanceThreshold &&
                                                velocity > velocityThreshold
                                        ) || (currentOffset.absoluteValue > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.player.seekToPreviousMediaItem()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.player.seekToNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 8.dp),
                ) {
                    // Album art thumbnail (larger, with shadow)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp), clip = false)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            }
                    ) {
                        mediaMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    sharedContentState = rememberSharedContentState(key = "album_art"),
                                                    animatedVisibilityScope = animatedVisibilityScope
                                                )
                                            }
                                        } else Modifier
                                    )
                            )
                        }

                        // Dark overlay when paused
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.Black.copy(alpha = overlayAlpha),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )

                        // Play icon overlay on thumbnail when paused
                        androidx.compose.animation.AnimatedVisibility(
                            visible = playbackState == Player.STATE_ENDED || !isPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                accentColor.copy(alpha = 0.4f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BrokenIcon(
                                    codePoint = if (playbackState == Player.STATE_ENDED) {
                                        BrokenIcons.refresh
                                    } else {
                                        BrokenIcons.play
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    size = 14.dp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Track info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val title = mediaMetadata?.title ?: "KuroMusic"
                        val artists = mediaMetadata?.artists?.joinToString { it.name } ?: ""

                        AnimatedContent(
                            targetState = title,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { t ->
                            DisableSelection {
                                Text(
                                    text = t,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }
                        }

                        if (artists.isNotBlank()) {
                            AnimatedContent(
                                targetState = artists,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "",
                            ) { a ->
                                DisableSelection {
                                    Text(
                                        text = a,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                }
                            }
                        }

                        if (error != null) {
                            Text(
                                text = "Error playing",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (!isNetworkConnected) {
                            Text(
                                text = "Offline",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Like button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { playerConnection.toggleLike() },
                        contentAlignment = Alignment.Center,
                    ) {
                        BrokenIcon(
                            codePoint = BrokenIcons.heart,
                            filled = currentSong?.song?.liked == true,
                            contentDescription = null,
                            tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 18.dp,
                        )
                    }

                    // Play/Pause button (Namida-style: prominent circle with gradient)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(
                                elevation = if (isPlaying) 6.dp else 0.dp,
                                shape = CircleShape,
                                clip = false
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        accentColor,
                                        accentColor.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable {
                                playerConnection.player.togglePlayPause()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BrokenIcon(
                            codePoint = if (isPlaying) BrokenIcons.pause else BrokenIcons.play,
                            contentDescription = null,
                            tint = Color.White,
                            size = 18.dp,
                        )
                    }

                    // Next button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSkipNext) {
                                playerConnection.player.seekToNext()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BrokenIcon(
                            codePoint = BrokenIcons.next,
                            contentDescription = null,
                            tint = if (canSkipNext) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            size = 18.dp,
                        )
                    }
                }
            }
        }

        // Swipe indicator icons
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 24.dp)
            ) {
                BrokenIcon(
                    codePoint = if (offsetXAnimatable.value > 0) BrokenIcons.previous else BrokenIcons.next,
                    contentDescription = null,
                    tint = accentColor.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    size = 24.dp,
                )
            }
        }

        // Progress bar at bottom of card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accentColor.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.6f),
                                accentColor
                            )
                        )
                    ),
            )
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: androidx.media3.common.PlaybackException?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    BrokenIcon(
                        codePoint = BrokenIcons.infoCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        size = 24.dp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
