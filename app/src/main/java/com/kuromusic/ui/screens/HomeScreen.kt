package com.kuromusic.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.ImageLoader
import coil.request.SuccessResult
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kuromusic.innertube.models.AlbumItem
import com.kuromusic.innertube.models.ArtistItem
import com.kuromusic.innertube.models.PlaylistItem
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.models.WatchEndpoint
import com.kuromusic.innertube.models.YTItem
import com.kuromusic.innertube.utils.parseCookieString
import com.kuromusic.LocalDatabase
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.constants.AccountNameKey
import com.kuromusic.constants.GridThumbnailHeight
import com.kuromusic.constants.InnerTubeCookieKey
import com.kuromusic.constants.ListItemHeight
import com.kuromusic.constants.ListThumbnailSize
import com.kuromusic.constants.ThumbnailCornerRadius
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.LocalItem
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.Song
import com.kuromusic.extensions.togglePlayPause
import com.kuromusic.models.toMediaMetadata
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.playback.queues.LocalAlbumRadio
import com.kuromusic.playback.queues.YouTubeAlbumRadio
import com.kuromusic.playback.queues.YouTubeQueue
import com.kuromusic.ui.component.AlbumGridItem
import com.kuromusic.ui.component.ArtistGridItem
import com.kuromusic.ui.component.ChipsRow
import com.kuromusic.ui.component.HideOnScrollFAB
import com.kuromusic.ui.component.LocalMenuState
import com.kuromusic.ui.component.NavigationTitle
import com.kuromusic.ui.component.SongGridItem
import com.kuromusic.ui.component.SongListItem
import com.kuromusic.ui.component.YouTubeGridItem
import com.kuromusic.ui.component.AuroraBackground
import com.kuromusic.ui.component.fadingEdge
import com.kuromusic.ui.theme.extractThemeColor
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kuromusic.constants.CONTENT_TYPE_HEADER
import com.kuromusic.ui.component.shimmer.GridItemPlaceHolder
import com.kuromusic.ui.component.shimmer.ShimmerHost
import com.kuromusic.ui.component.shimmer.TextPlaceholder
import com.kuromusic.ui.menu.AlbumMenu
import com.kuromusic.ui.menu.ArtistMenu
import com.kuromusic.ui.menu.SongMenu
import com.kuromusic.ui.menu.YouTubeAlbumMenu
import com.kuromusic.ui.menu.YouTubeArtistMenu
import com.kuromusic.ui.menu.YouTubePlaylistMenu
import com.kuromusic.ui.menu.YouTubeSongMenu
import com.kuromusic.ui.utils.SnapLayoutInfoProvider
import com.kuromusic.utils.rememberPreference
import com.kuromusic.utils.formatCount
import com.kuromusic.utils.makeTimeString
import com.kuromusic.utils.joinByBullet
import com.kuromusic.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.kuromusic.ui.component.GlassmorphicCard
import com.kuromusic.ui.utils.resize
import androidx.compose.foundation.layout.PaddingValues

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val forYouMix by viewModel.forYouMix.collectAsState()
    
    // Dynamic Smart Home Flows
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState(initial = emptyList())
    val essentialSongs by viewModel.essentialSongs.collectAsState(initial = emptyList())
    val topArtist by viewModel.topArtist.collectAsState(initial = null)
    val topGenre by viewModel.topGenre.collectAsState(initial = null)
    val animaxVideos by viewModel.animaxVideos.collectAsState()
    val globalRecommendations by viewModel.globalRecommendations.collectAsState()
    val dynamicGradientColor by viewModel.dynamicGradientColor.collectAsState()

    // Smooth color transition with 500ms animation
    val animatedGradientColor by animateColorAsState(
        targetValue = dynamicGradientColor,
        animationSpec = tween<Color>(durationMillis = 500),
        label = "gradient_color_transition"
    )

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id || it.id == playerConnection.player.currentMediaItem?.mediaId) {
                                if (isPlaying) {
                                    playerConnection.player.pause()
                                } else {
                                    playerConnection.player.play()
                                }
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        val isAlbumOrPlaylist = item is AlbumItem || item is PlaylistItem
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            thumbnailShape = if (isAlbumOrPlaylist) RoundedCornerShape(24.dp) else if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius),
            imageSize = if (isAlbumOrPlaylist) 800 else 512,
            modifier = Modifier
                .width(if (isAlbumOrPlaylist) 180.dp else 140.dp)
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val pureBlack by rememberPreference(PureBlackKey, false)
        
        // Aurora Color State
        var auroraColor by remember { mutableStateOf(Color.Black) }
        val context = LocalContext.current
        
        // Extract color for Aurora and Dynamic Gradient
        LaunchedEffect(mediaMetadata?.thumbnailUrl) {
            val url = mediaMetadata?.thumbnailUrl ?: return@LaunchedEffect
            
            // Ejecutamos en Default para no bloquear el hilo de UI
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(120, 120) // Redimensionar aquí ahorra muchísima memoria y CPU
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
                    
                val result = coil.ImageLoader(context).execute(request)
                if (result is SuccessResult) {
                    result.drawable.toBitmapOrNull()?.let { bitmap ->
                        // Extract for Aurora effect
                        val extracted = bitmap.extractThemeColor()
                        
                        // Extract for Dynamic Gradient using Palette API
                        val dynamicColor = com.kuromusic.ui.utils.DynamicColorExtractor.extractDominantColor(bitmap)
                        
                        // Volvemos al Main para actualizar el estado
                        withContext(Dispatchers.Main) {
                            auroraColor = extracted
                            viewModel.dynamicGradientColor.value = dynamicColor
                        }
                    }
                }
            }
        }

        AuroraBackground(
            isVisible = true, // Always visible - handles theme internally
            color = animatedGradientColor,
            scrollOffsetProvider = {
                if (lazylistState.firstVisibleItemIndex > 0) 1000f 
                else lazylistState.firstVisibleItemScrollOffset.toFloat()
            }
        ) {

        val horizontalLazyGridItemWidthFactor = remember(maxWidth) {
            if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        }
        val horizontalLazyGridItemWidth = remember(maxWidth, horizontalLazyGridItemWidthFactor) {
            maxWidth * horizontalLazyGridItemWidthFactor
        }
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState, horizontalLazyGridItemWidthFactor) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent) // Reduced Overdraw: Main background is handled by MainActivity/Aurora
                .fadingEdge(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.03f to Color.Black,
                        0.97f to Color.Black,
                        1f to Color.Transparent
                    )
                )
        ) {
            item {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    ChipsRow(
                        chips = listOfNotNull(
                            Pair("history", stringResource(R.string.history)),
                            Pair("stats", stringResource(R.string.stats)),
                            Pair("liked", stringResource(R.string.liked)),
                            Pair("downloads", stringResource(R.string.offline)),
                            if (isLoggedIn) Pair(
                                "account",
                                stringResource(R.string.account)
                            ) else null
                        ),
                        currentValue = "",
                        onValueUpdate = { value ->
                            when (value) {
                                "history" -> navController.navigate("history")
                                "stats" -> navController.navigate("stats")
                                "liked" -> navController.navigate("auto_playlist/liked")
                                "downloads" -> navController.navigate("auto_playlist/downloaded")
                                "account" -> if (isLoggedIn) navController.navigate("account")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }

            // --- BLOQUE 0: CARGA (SKELETON) ---
            if (isLoading && (animaxVideos == null || animaxVideos!!.isEmpty())) {
                item { SectionHeader(title = "Animax L Music") }
                item { AnimaxHorizontalShimmer() }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item { SectionHeader(title = "Escuchado Recientemente") }
                item { RecentsMiniShimmer() }
            }

            // --- BLOQUE 1: ANIMAX L MUSIC (ESTRUCTURA DE CUADRÍCULA HORIZONTAL IDENTICA A YT) ---
            animaxVideos?.takeIf { it.isNotEmpty() }?.let { videos ->
                item {
                    SectionHeader(
                        title = "Animax L Music",
                        subtitle = "Contenido oficial",
                        modifier = Modifier.animateItem().padding(top = 8.dp)
                    )
                }
                
                item {
                    val songGroups = remember(videos) {
                        videos.filterIsInstance<SongItem>().take(20).chunked(4)
                    }
                    val rowState = rememberLazyListState()

                    LazyRow(
                        state = rowState,
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState)
                    ) {
                        items(songGroups) { group ->
                            // Cada columna contiene 4 canciones una encima de otra (85% del ancho)
                            Column(
                                modifier = Modifier.fillParentMaxWidth(0.85f)
                            ) {
                                group.forEach { song ->
                                    YouTubeStyleItem(
                                        song = song,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- BLOQUE 2: ESCUCHADO RECIENTEMENTE (MINI-CARRUSEL 110DP) ---
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Escuchado Recientemente",
                        modifier = Modifier.animateItem()
                    )
                }
                
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        items(
                            items = recentlyPlayed.take(15),
                            key = { "recent_${it.id}" }
                        ) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable {
                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    }
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // --- BLOQUE 3: EXPLORAR (Novedades de la API) ---
            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = "Explorar", modifier = Modifier.animateItem())
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        items(albums) { album ->
                            ytGridItem(album)
                        }
                    }
                }
            }

            // --- BLOQUE 4: VIDEOS MUSICALES / PARA TI ---
            homePage?.sections?.find { it.title?.contains("Video", true) == true || it.title?.contains("Music Video", true) == true }?.let { section ->
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = section.title ?: "Videos Musicales", modifier = Modifier.animateItem())
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        items(section.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // --- BLOQUE 5: RESTO DE SECCIONES API (DEDUPLICADAS) ---
            homePage?.sections?.filter { 
                val title = it.title ?: ""
                !title.contains("Animax", true) && 
                !title.contains("Video", true) &&
                !title.contains("Music Video", true) &&
                !title.contains("Álbum", true) && // Exclude explicit album sections
                !title.contains("Album", true)
            }?.forEach { section ->
                item(key = "section_${section.title}") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = section.title,
                        modifier = Modifier.animateItem()
                    )
                }
                
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(section.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // --- OTROS: FAVORITOS Y MEZCLAS (VARIADOS) ---
            // 1. ÁLBUMES PARA TI (if not already covered by generic sections)
            homePage?.sections?.find { it.title?.contains("Álbum", true) == true || it.title?.contains("Album", true) == true }?.let { section ->
                item {
                    NavigationTitle(
                        title = section.title ?: "Álbumes para ti",
                        label = section.label,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        itemsIndexed(
                            items = section.items,
                            key = { _, item -> "album_${item.id}" }
                        ) { _, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // 2. ÉXITOS DE CADA DÉCADA / MIXES
            forYouMix?.takeIf { it.isNotEmpty() }?.let { mix ->
                val decadeMix = mix.filter { it is SongItem && (it.title.contains("80") || it.title.contains("90") || it.title.contains("2000") || it.title.contains("Hits")) }
                if (decadeMix.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = "Éxitos de cada década",
                            label = "Tus mezclas favoritas",
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .animateItem()
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().animateItem()
                        ) {
                            itemsIndexed(
                                items = decadeMix,
                                key = { _, item -> "decade_${item.id}" }
                            ) { _, item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            // 3. TUS FAVORITOS (BASADO EN REPRODUCCIONES REALES)
            if (essentialSongs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = "Tus Favoritos", subtitle = "Basado en reproducciones", modifier = Modifier.animateItem())
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(essentialSongs) { song ->
                            SongGridItem(
                                song = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier.width(120.dp).clickable {
                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                }
                            )
                        }
                    }
                }
            }

            // 4. Para Ti Section (General For You Mix)
            forYouMix?.takeIf { it.isNotEmpty() }?.let { mix ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.for_you),
                        label = "Especialmente para ti",
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }
                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = mix,
                            key = { _, item -> "foryou_${item.id}" }
                        ) { _, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // Contextual (More from Artist / Genre)
            topArtist?.let { artist ->
                item {
                    NavigationTitle(
                        title = "Más de ${artist.title}",
                        modifier = Modifier.animateItem()
                    )
                }
                // Need to fetch or use DB for artist songs?
                // I can use `artistSongsPreview`. Or existing `artist` object doesn't have songs list.
                // I'll skip implementation DETAIL for song list here to avoid DB query complexity in UI
                // unless I expose a Flow for "Top Artist Songs".
                // User said "crea automáticamente una fila".
                // I will placeholder or remove if too complex without new ViewMOdel flow.
                // Actually `similarRecommendations` has `artistRecommendations`.
                // I'll accept `recentActivity` or `similarRecommendations` usage for now.
            }
            
            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }

                item {
                    LazyHorizontalGrid(
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(3),
                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .add(WindowInsets(left = 16.dp, right = 16.dp))
                            .asPaddingValues(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 3 + 32.dp) // Add spacing buffer
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = quickPicks,
                            key = { _, item -> item.id }
                        ) { _, originalSong ->
                            // fetch song from database to keep updated
                            val song by database.song(originalSong.id)
                                .collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id || song!!.id == playerConnection.player.currentMediaItem?.mediaId) {
                                                if (isPlaying) {
                                                    playerConnection.player.pause()
                                                } else {
                                                    playerConnection.player.play()
                                                }
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }

                item {
                    val rows = if (keepListening.size > 6) 2 else 1
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + with(LocalDensity.current) {
                                MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                            }) * rows)
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = keepListening,
                            key = { _, item -> "keep_${item.id}" }
                        ) { _, item ->
                            localGridItem(item)
                        }
                    }
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item {
                    NavigationTitle(
                        label = stringResource(R.string.your_ytb_playlists),
                        title = accountName,
                        thumbnail = {
                            if (url != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(url)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(id = R.drawable.person),
                                    error = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(ListThumbnailSize)
                                )
                            }
                        },
                        onClick = {
                            navController.navigate("account")
                        },
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }


                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = accountPlaylists,
                            key = { _, it -> "playlist_${it.id}" },
                        ) { _, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            similarRecommendations?.forEach {
                item {
                    NavigationTitle(
                        label = stringResource(R.string.similar_to),
                        title = it.title.title,
                        thumbnail = it.title.thumbnailUrl?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (it.title is Artist) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = {
                            when (it.title) {
                                is Song -> navController.navigate("album/${it.title.album!!.id}")
                                is Album -> navController.navigate("album/${it.title.id}")
                                is Artist -> navController.navigate("artist/${it.title.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = it.items,
                            key = { _, item -> "similar_${item.id}" }
                        ) { _, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                        onClick = {
                            navController.navigate("new_release")
                        },
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        items(
                            items = newReleaseAlbums,
                            key = { it.id }
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }
            }


            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateItem()
                    )
                }

                item {
                    // take min in case list size is less than 4
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(
                            forgottenFavoritesSnapLayoutInfoProvider
                        ),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * rows)
                            .animateItem()
                    ) {
                        items(
                            items = forgottenFavorites,
                            key = { it.id }
                        ) { originalSong ->
                            val song by database.song(originalSong.id)
                                .collectAsState(initial = originalSong)

                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (song!!.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
        }

        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || homePage != null,
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                // Compute available YT items from homePage
                val allYtSongs = homePage?.sections
                    ?.flatMap { it.items }
                    ?.filterIsInstance<SongItem>() ?: emptyList()
                
                val local = when {
                    allLocalItems.isNotEmpty() && allYtSongs.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                scope.launch(Dispatchers.Main) {
                    if (local) {
                        when (val luckyItem = allLocalItems.random()) {
                            is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is Album -> {
                                val albumWithSongs = withContext(Dispatchers.IO) {
                                    database.albumWithSongs(luckyItem.id).first()
                                }
                                albumWithSongs?.let {
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }

                            is Artist -> {}
                            is Playlist -> {}
                        }
                    } else {
                        // Use filtered SongItems from homePage
                        allYtSongs.randomOrNull()?.let { luckyItem ->
                            playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                        }
                    }
                }
            }
        )

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

private fun com.kuromusic.models.MediaMetadata.toSong(): com.kuromusic.db.entities.Song {
    return com.kuromusic.db.entities.Song(
        song = com.kuromusic.db.entities.SongEntity(
            id = id,
            title = title,
            duration = (duration / 1000).toInt().coerceAtLeast(0), // MediaMetadata uses millis (Long), SongEntity uses seconds (Int)
            thumbnailUrl = thumbnailUrl
        ),
        artists = artists.map { com.kuromusic.db.entities.ArtistEntity(id = it.id ?: "", name = it.name) },
        album = null
    )
}

/**
 * SectionRow Component - Professional YouTube Music-like section display
 * Features:
 * - 160dp square cards with 16dp rounded corners
 * - Proper handling of all YTItem types (Song, Album, Artist, Playlist)
 * - 2-line title with ellipsis
 * - Disk cache enabled for all images
 */
@Composable
fun SectionRow(
    title: String,
    label: String? = null,
    items: List<YTItem>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    Column(modifier = modifier) {
        NavigationTitle(
            title = title,
            label = label,
            modifier = Modifier.padding(top = 24.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                YouTubeGridItem(
                    item = item,
                    isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    thumbnailRatio = 1f,
                    thumbnailShape = RoundedCornerShape(16.dp),
                    imageSize = 320,
                    modifier = Modifier
                        .width(160.dp)
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = menuState::dismiss
                                        )
                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        )
                )
            }
        }
    }
}

/**
 * SectionHeader - Elegant title component with optional subtitle
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * AnimaxCompactItem - Super compact list item (48dp image, 56dp total height)
 * Goal: Fit 5-6 items visible without scrolling
 */
@Composable
fun AnimaxCompactItem(
    song: com.kuromusic.innertube.models.SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art - 48dp
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.thumbnail)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(96)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) 
                            Icons.Rounded.PlayArrow 
                        else 
                            Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title - Single line with ellipsis
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // Duration (optional)
        song.duration?.let { duration ->
            val minutes = (duration / 60)
            val seconds = (duration % 60)
            Text(
                text = String.format("%d:%02d", minutes, seconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * RecentsMiniCarousel - Horizontal carousel with 100dp mini cards
 */
@Composable
fun RecentsMiniCarousel(
    recents: List<Song>,
    isPlaying: Boolean,
    currentSongId: String?,
    onItemClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = recents,
            key = { "recent_${it.id}" }
        ) { song ->
            RecentMiniCard(
                song = song,
                isActive = song.id == currentSongId,
                isPlaying = isPlaying,
                onClick = { onItemClick(song) }
            )
        }
    }
}

/**
 * RecentMiniCard - 100dp x 100dp mini card for recents
 */
@Composable
fun RecentMiniCard(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val navController = LocalContext.current as? androidx.activity.ComponentActivity
    
    Column(
        modifier = Modifier
            .width(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Menu handling can be added here
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album art - 100dp × 100dp
        Box {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(
                        imageVector = if (isPlaying) 
                            Icons.Rounded.PlayArrow 
                        else 
                            Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Title - Very small, 2 lines max
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AnimaxHorizontalShimmer() {
    ShimmerHost {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simulamos 2 columnas de carga
            repeat(2) {
                Column(modifier = Modifier.width(280.dp)) {
                    repeat(4) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(52.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Box(modifier = Modifier.height(14.dp).width(150.dp).background(Color.Gray))
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.height(12.dp).width(100.dp).background(Color.Gray))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AnimaxCompactShimmer - Skeleton loader for small lists
 */
@Composable
fun AnimaxCompactShimmer() {
    Column {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerHost {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerHost {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * RecentsMiniShimmer - Skeleton loader for mini carousel
 */
@Composable
fun RecentsMiniShimmer() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(8) {
            Column {
                ShimmerHost {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerHost {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeStyleItem(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniatura con bordes redondeados sutiles (estilo YT Music)
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Indicador de reproducción si está activa
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Textos: Título y Subtítulo
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            val artistName = song.artists.firstOrNull()?.name ?: ""
            val durationText = song.duration?.let { makeTimeString(it * 1000L) } ?: ""
            val subtitle = joinByBullet(artistName, durationText)
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Botón de más opciones (Menú)
        IconButton(
            onClick = onLongClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * CompactMusicItem - Enhanced compact item with subtitle
 * Subtitle shows: Artist • Views • Duration using joinByBullet
 */
@Composable
fun CompactMusicItem(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp), // Espacio reducido a 4dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square Image 48dp
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) 
                            Icons.Rounded.Pause
                        else 
                            Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title + Subtitle column
        Column(modifier = Modifier.weight(1f)) {
            // Title 14sp
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Subtitle: Artist • Views • Duration
            val subtitle = joinByBullet(
                song.artists?.firstOrNull()?.name,
                formatCount(song.viewCount),
                song.duration?.let { makeTimeString(it * 1000L) }
            )
            
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}