package com.kuromusic.ui.screens.library

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.constants.AlbumViewTypeKey
import com.kuromusic.constants.CONTENT_TYPE_HEADER
import com.kuromusic.constants.CONTENT_TYPE_PLAYLIST
import com.kuromusic.constants.GridItemSize
import com.kuromusic.constants.GridItemsSizeKey
import com.kuromusic.constants.GridThumbnailHeight
import com.kuromusic.constants.LibraryViewType
import com.kuromusic.constants.MixSortDescendingKey
import com.kuromusic.constants.MixSortType
import com.kuromusic.constants.MixSortTypeKey
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.LocalItem
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.PlaylistEntity
import com.kuromusic.extensions.reversed
import com.kuromusic.ui.component.AlbumGridItem
import com.kuromusic.ui.component.AlbumListItem
import com.kuromusic.ui.component.ArtistGridItem
import com.kuromusic.ui.component.ArtistListItem
import com.kuromusic.ui.component.LocalMenuState
import com.kuromusic.ui.component.PlaylistGridItem
import com.kuromusic.ui.component.PlaylistListItem
import com.kuromusic.ui.component.SortHeader
import com.kuromusic.ui.menu.AlbumMenu
import com.kuromusic.ui.menu.ArtistMenu
import com.kuromusic.ui.menu.PlaylistMenu
import com.kuromusic.utils.rememberEnumPreference
import com.kuromusic.utils.rememberPreference
import com.kuromusic.viewmodels.LibraryMixViewModel
import com.kuromusic.ui.component.LibraryListItem
import com.kuromusic.ui.component.ArtistCircleItem
import com.kuromusic.ui.component.LibraryArtistItem
import com.kuromusic.utils.getPlaylistImageUri
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

    val context = LocalContext.current
    var sortType by rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    var sortDescending by rememberPreference(MixSortDescendingKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    
    val likedSongsCount by viewModel.likedSongsCount.collectAsState(initial = 0)
    val downloadedSongsCount by viewModel.downloadedSongsCount.collectAsState(initial = 0)

    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val playlist = viewModel.playlists.collectAsState()

    val allItems by remember(albums.value, artist.value, playlist.value, sortType, sortDescending) {
        derivedStateOf {
            val items: List<LocalItem> = albums.value + artist.value + playlist.value
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            when (sortType) {
                MixSortType.CREATE_DATE ->
                    items.sortedBy { item ->
                        when (item) {
                            is Album -> item.album.bookmarkedAt
                            is Artist -> item.artist.bookmarkedAt
                            is Playlist -> item.playlist.createdAt
                            else -> LocalDateTime.now()
                        }
                    }

                MixSortType.NAME ->
                    items.sortedWith(
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
                    items.sortedBy { item ->
                        when (item) {
                            is Album -> item.album.lastUpdateTime
                            is Artist -> item.artist.lastUpdateTime
                            is Playlist -> item.playlist.lastUpdateTime
                            else -> LocalDateTime.now()
                        }
                    }
                else -> items
            }.reversed(sortDescending)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    

    val isScrolling by remember { derivedStateOf { lazyListState.isScrollInProgress } }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "filter") {
                filterContent()
            }
            
            // ... (rest of items)

            item(key = "header") {
                SortHeader<MixSortType>(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = { sortType = it },
                    onSortDescendingChange = { sortDescending = it },
                    sortTypeText = { type ->
                        when (type) {
                            MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                            MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                            MixSortType.NAME -> R.string.sort_by_name
                            else -> R.string.sort_by_create_date
                        }
                    },
                )
            }

            // SECCIÓN A: ANCLADOS (PINNED)
            item(key = "pinned_liked") {
                LibraryListItem(
                    title = stringResource(R.string.liked),
                    subtitle = "Playlist • $likedSongsCount ${stringResource(R.string.songs).lowercase()}",
                    iconRes = R.drawable.favorite,
                    gradientColors = listOf(Color(0xFFFF0D86), Color(0xFF6B11CB)),
                    onClick = { navController.navigate("auto_playlist/liked") }
                )
            }

            item(key = "pinned_offline") {
                LibraryListItem(
                    title = stringResource(R.string.offline),
                    subtitle = "Playlist • $downloadedSongsCount ${stringResource(R.string.songs).lowercase()}",
                    iconRes = R.drawable.download,
                    gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
                    onClick = { navController.navigate("auto_playlist/downloaded") }
                )
            }

            item(key = "pinned_stats") {
                LibraryListItem(
                    title = "Estadísticas",
                    subtitle = "Top canciones, artistas y álbumes",
                    iconRes = R.drawable.trending_up,
                    gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                    onClick = { navController.navigate("stats") }
                )
            }

            // LISTA HÍBRIDA (Artistas, Playlists, Álbumes)
            items(
                items = allItems,
                key = { (it as LocalItem).id },
                contentType = {
                    when (it) {
                        is Artist -> "artist"
                        is Playlist -> "playlist"
                        is Album -> "album"
                        else -> "other"
                    }
                }
            ) { item ->
                Box(modifier = Modifier.animateItem()) {
                    when (item) {
                    is Artist -> {
                        LibraryArtistItem(
                            artistName = item.artist.name,
                            thumbnailUrl = item.artist.thumbnailUrl,
                            isScrolling = isScrolling,
                            onClick = { navController.navigate("artist/${item.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        )
                    }

                    is Playlist -> {
                        LibraryListItem(
                            title = item.playlist.name,
                            subtitle = "Playlist • ${item.songCount} ${stringResource(R.string.songs).lowercase()}",
                            imageUrl = item.playlist.browseId?.let { getPlaylistImageUri(context, it).toString() } ?: item.playlist.id.let { getPlaylistImageUri(context, it).toString() },
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
                                    }
                                ) {
                                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                }
                            },
                            onClick = { navController.navigate("local_playlist/${item.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        )
                    }

                    is Album -> {
                        LibraryListItem(
                            title = item.album.title,
                            subtitle = "Album • ${item.artists.joinToString { it.name }}",
                            imageUrl = item.album.thumbnailUrl,
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
                                    }
                                ) {
                                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                }
                            },
                            onClick = { navController.navigate("album/${item.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        )
                    }
                    else -> {} // Exhaustive check
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            modifier = Modifier
                .size(if (isFullWidth) 100.dp else 70.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 15.dp)
                .rotate(15f)
        )
    }
}
