package com.grp2.kuromusic.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grp2.kuromusic.LocalPlayerAwareWindowInsets
import com.grp2.kuromusic.LocalPlayerConnection
import com.grp2.kuromusic.R
import com.grp2.kuromusic.constants.AlbumViewTypeKey
import com.grp2.kuromusic.constants.CONTENT_TYPE_HEADER
import com.grp2.kuromusic.constants.CONTENT_TYPE_PLAYLIST
import com.grp2.kuromusic.constants.GridItemSize
import com.grp2.kuromusic.constants.GridItemsSizeKey
import com.grp2.kuromusic.constants.GridThumbnailHeight
import com.grp2.kuromusic.constants.LibraryViewType
import com.grp2.kuromusic.constants.MixSortDescendingKey
import com.grp2.kuromusic.constants.MixSortType
import com.grp2.kuromusic.constants.MixSortTypeKey
import com.grp2.kuromusic.db.entities.Album
import com.grp2.kuromusic.db.entities.Artist
import com.grp2.kuromusic.db.entities.Playlist
import com.grp2.kuromusic.db.entities.PlaylistEntity
import com.grp2.kuromusic.extensions.reversed
import com.grp2.kuromusic.ui.component.AlbumGridItem
import com.grp2.kuromusic.ui.component.AlbumListItem
import com.grp2.kuromusic.ui.component.ArtistGridItem
import com.grp2.kuromusic.ui.component.ArtistListItem
import com.grp2.kuromusic.ui.component.LocalMenuState
import com.grp2.kuromusic.ui.component.PlaylistGridItem
import com.grp2.kuromusic.ui.component.PlaylistListItem
import com.grp2.kuromusic.ui.component.SortHeader
import com.grp2.kuromusic.ui.menu.AlbumMenu
import com.grp2.kuromusic.ui.menu.ArtistMenu
import com.grp2.kuromusic.ui.menu.PlaylistMenu
import com.grp2.kuromusic.utils.rememberEnumPreference
import com.grp2.kuromusic.utils.rememberPreference
import com.grp2.kuromusic.viewmodels.LibraryMixViewModel
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val likedSongsCount by viewModel.likedSongsCount.collectAsState(initial = 0)
    val downloadedSongsCount by viewModel.downloadedSongsCount.collectAsState(initial = 0)
    val cachedSongsCount by viewModel.cachedSongsCount.collectAsState(initial = 0)

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    var allItems = albums.value + artist.value + playlist.value
    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }

            MixSortType.NAME ->
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )

            MixSortType.LAST_UPDATED ->
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
        }.reversed(sortDescending)

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            when (viewType) {
                                LibraryViewType.LIST -> R.drawable.list
                                LibraryViewType.GRID -> R.drawable.grid_view
                            },
                        ),
                    contentDescription = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewType) {
            LibraryViewType.LIST ->
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }


                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        LibraryCategoryCard(
                            title = stringResource(R.string.liked),
                            subtitle = "$likedSongsCount ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.favorite,
                            gradientColors = listOf(Color(0xFFFF0D86), Color(0xFF6B11CB)),
                            isFullWidth = true,
                            onClick = { navController.navigate("auto_playlist/liked") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LibraryCategoryCard(
                                title = stringResource(R.string.offline),
                                subtitle = "$downloadedSongsCount ${stringResource(R.string.songs).lowercase()}",
                                icon = R.drawable.download,
                                gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
                                onClick = { navController.navigate("auto_playlist/downloaded") },
                                modifier = Modifier.weight(1f)
                            )
                            LibraryCategoryCard(
                                title = stringResource(R.string.my_top) + " $topSize",
                                subtitle = "$topSize ${stringResource(R.string.songs).lowercase()}",
                                icon = R.drawable.trending_up,
                                gradientColors = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
                                onClick = { navController.navigate("top_playlist/$topSize") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        LibraryCategoryCard(
                            title = stringResource(R.string.cached_playlist),
                            subtitle = "$cachedSongsCount ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.tune,
                            gradientColors = listOf(Color(0xFF30E8BF), Color(0xFFFF8235)),
                            onClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(0.5f)
                        )
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    items(
                        items = allItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("local_playlist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        PlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }

            LibraryViewType.GRID ->
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }


                    item(
                        key = "likedPlaylist",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        LibraryCategoryCard(
                            title = stringResource(R.string.liked),
                            subtitle = "$likedSongsCount ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.favorite,
                            gradientColors = listOf(Color(0xFFFF0D86), Color(0xFF6B11CB)),
                            isFullWidth = true,
                            onClick = { navController.navigate("auto_playlist/liked") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item(
                        key = "downloadedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        LibraryCategoryCard(
                            title = stringResource(R.string.offline),
                            subtitle = "$downloadedSongsCount ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.download,
                            gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
                            onClick = { navController.navigate("auto_playlist/downloaded") },
                            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        LibraryCategoryCard(
                            title = stringResource(R.string.my_top) + " $topSize",
                            subtitle = "$topSize ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.trending_up,
                            gradientColors = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
                            onClick = { navController.navigate("top_playlist/$topSize") },
                            modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }
                    item(
                        key = "cachePlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        LibraryCategoryCard(
                            title = stringResource(R.string.cached_playlist),
                            subtitle = "$cachedSongsCount ${stringResource(R.string.songs).lowercase()}",
                            icon = R.drawable.tune,
                            gradientColors = listOf(Color(0xFF30E8BF), Color(0xFFFF8235)),
                            onClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        items = allItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("local_playlist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        PlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                    context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                                )
                            }

                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }
                }
        }
    }
}
@Composable
fun LibraryCategoryCard(
    title: String,
    subtitle: String,
    icon: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false
) {
    Box(
        modifier = modifier
            .height(if (isFullWidth) 140.dp else 110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(onClick = onClick)
    ) {
        // Glassmorphism overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = 0.05f))
        )
        
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(if (isFullWidth) 100.dp else 70.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 15.dp)
                .rotate(15f)
        )
    }
}
