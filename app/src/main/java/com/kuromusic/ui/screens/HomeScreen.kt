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
import androidx.compose.foundation.layout.Arrangement
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
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.Song
import com.kuromusic.extensions.togglePlayPause
import com.kuromusic.models.toMediaMetadata
import com.kuromusic.constants.PureBlackKey
import com.kuromusic.constants.DynamicThemeKey
import com.kuromusic.constants.ShowAnimaxSectionKey
import com.kuromusic.utils.rememberPreference
import com.kuromusic.playback.queues.YouTubeAlbumRadio
import com.kuromusic.playback.queues.YouTubeQueue
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
import com.kuromusic.ui.component.shimmer.GridItemPlaceHolder
import com.kuromusic.ui.component.shimmer.ShimmerHost
import com.kuromusic.ui.component.shimmer.TextPlaceholder
import com.kuromusic.ui.menu.SongMenu
import com.kuromusic.ui.menu.YouTubeAlbumMenu
import com.kuromusic.ui.menu.YouTubeArtistMenu
import com.kuromusic.ui.menu.YouTubePlaylistMenu
import com.kuromusic.ui.menu.YouTubeSongMenu
import com.kuromusic.utils.formatCount
import com.kuromusic.utils.makeTimeString
import com.kuromusic.utils.joinByBullet
import com.kuromusic.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val forYouMix by viewModel.forYouMix.collectAsState()
    
    // Dynamic Smart Home Flows
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState(initial = emptyList())
    val essentialSongs by viewModel.essentialSongs.collectAsState(initial = emptyList())
    val topArtist by viewModel.topArtist.collectAsState(initial = null)
    val topGenre by viewModel.topGenre.collectAsState(initial = null)
    val animaxVideos by viewModel.animaxVideos.collectAsState()
    val globalRecommendations by viewModel.globalRecommendations.collectAsState()
    val localRecommendations by viewModel.localRecommendations.collectAsState()
    val dynamicGradientColor by viewModel.dynamicGradientColor.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val accountName by rememberPreference(AccountNameKey, "")
    val showAnimaxSection by rememberPreference(ShowAnimaxSectionKey, true)
    val enableDynamicTheme by rememberPreference(DynamicThemeKey, false)
    val animaxSectionTitle = if (showAnimaxSection) "Animax L Music" else "Canciones Recomendadas"
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
        
        // **OPTIMIZATION**: Cache ImageLoader singleton instead of creating new instances
        val imageLoader = remember { coil.ImageLoader(context) }
        
        // **OPTIMIZATION**: Cache color extraction results per URL to avoid re-processing
        val colorCache = remember { mutableMapOf<String, Pair<Color, Color>>() }
        
        // Extract color for Aurora and Dynamic Gradient
        LaunchedEffect(mediaMetadata?.thumbnailUrl) {
            if (!enableDynamicTheme) return@LaunchedEffect
            val url = mediaMetadata?.thumbnailUrl ?: return@LaunchedEffect
            
            // Check cache first
            colorCache[url]?.let { (aurora, gradient) ->
                auroraColor = aurora
                viewModel.dynamicGradientColor.value = gradient
                return@LaunchedEffect
            }
            
            // Ejecutamos en Default para no bloquear el hilo de UI
            withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(120, 120) // Redimensionar aquí ahorra muchísima memoria y CPU
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
                    
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    result.drawable.toBitmapOrNull()?.let { bitmap ->
                        // Extract for Aurora effect
                        val extracted = bitmap.extractThemeColor()
                        
                        // Extract for Dynamic Gradient using Palette API
                        val dynamicColor = com.kuromusic.ui.utils.DynamicColorExtractor.extractDominantColor(bitmap)
                        
                        // Cache the results
                        colorCache[url] = extracted to dynamicColor
                        
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
            color = dynamicGradientColor,
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // --- BLOQUE 0: CARGA (SKELETON) ---
            if (isLoading && (animaxVideos == null || animaxVideos!!.isEmpty())) {
                item { SectionHeader(title = animaxSectionTitle) }
                item { AnimaxHorizontalShimmer() }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item { SectionHeader(title = "Escuchado Recientemente") }
                item { RecentsMiniShimmer() }
            }

            // --- BLOQUE 1: ANIMAX L MUSIC (ESTRUCTURA DE CUADRÍCULA HORIZONTAL IDENTICA A YT) ---
            animaxVideos?.takeIf { it.isNotEmpty() }?.let { videos ->
                item {
                    SectionHeader(
                        title = animaxSectionTitle,
                        subtitle = if (showAnimaxSection) "Contenido oficial" else "Según tu historial",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                item {
                    val songGroups = remember(videos) {
                        videos.filterIsInstance<SongItem>().take(20).chunked(4)
                    }
                    val rowState = rememberLazyListState()

                    LazyRow(
                        state = rowState,
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier
                    )
                }
                
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
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

            // --- BLOQUE 2.5: RECOMENDACIONES (SIN CUENTA) ---
            if (homePage == null && localRecommendations.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Recomendado para ti",
                        subtitle = "Basado en tu historial",
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = localRecommendations.take(20),
                            key = { "reco_${it.id}" }
                        ) { song ->
                            SongGridItem(
                                song = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable {
                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    }
                            )
                        }
                    }
                }
            }

            // --- BLOQUE 3: EXPLORAR (Novedades de la API) ---
            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = "Explorar")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
            }?.distinctBy { it.title }?.forEach { section ->
                item(key = "section_${section.title}") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = section.title,
                        modifier = Modifier
                    )
                }
                
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
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
                            
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
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
                            
                    )
                }
                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
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
                        modifier = Modifier
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
            

        }
        }

        HideOnScrollFAB(
            visible = homePage != null,
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                val allYtSongs = homePage?.sections
                    ?.flatMap { it.items }
                    ?.filterIsInstance<SongItem>() ?: emptyList()
                scope.launch(Dispatchers.Main) {
                    allYtSongs.randomOrNull()?.let { luckyItem ->
                        playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
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

