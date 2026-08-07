package com.kuromusic.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kuromusic.LocalDatabase
import com.kuromusic.LocalDownloadUtil
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.constants.AlbumSortType
import com.kuromusic.constants.ArtistSortType
import com.kuromusic.constants.LibraryFilter
import com.kuromusic.constants.SongSortType
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.Song
import com.kuromusic.extensions.toMediaItem
import com.kuromusic.playback.queues.ListQueue
import com.kuromusic.ui.icons.BrokenIcon
import com.kuromusic.ui.icons.BrokenIcons
import kotlinx.coroutines.flow.flowOf

@Composable
fun OfflineHomeScreen(
    navController: NavController,
    onFilterChange: (LibraryFilter) -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloads by downloadUtil.downloads.collectAsState()
    val likedSongs by remember { database.likedSongs(SongSortType.CREATE_DATE, false) }.collectAsState(initial = emptyList())
    val albums by remember { database.albums(AlbumSortType.CREATE_DATE, false) }.collectAsState(initial = emptyList())
    val artists by remember { database.artists(ArtistSortType.NAME, false) }.collectAsState(initial = emptyList())

    val completedIds = remember(downloads) {
        downloads.filterValues { it.state == Download.STATE_COMPLETED }.keys.toList()
    }

    val downloadedSongs by remember(database, completedIds) {
        if (completedIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            database.songsByIds(completedIds)
        }
    }.collectAsState(initial = emptyList())

    val hasContent = downloadedSongs.isNotEmpty() || likedSongs.isNotEmpty() || albums.isNotEmpty() || artists.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        OfflineHeader(
            onBack = { navController.popBackStack() },
        )

        if (!hasContent) {
            EmptyOfflineView()
            return@Column
        }

        if (downloadedSongs.isNotEmpty()) {
            SectionHeader(
                title = "Downloads",
                count = downloadedSongs.size,
                onClick = { onFilterChange(LibraryFilter.DOWNLOADS) },
            )
            DownloadsRow(
                songs = downloadedSongs,
                onSongClick = { song, index ->
                    playerConnection.playQueue(
                        ListQueue(
                            title = "Downloads",
                            items = downloadedSongs.map { it.toMediaItem() },
                            startIndex = index,
                        ),
                    )
                },
            )
        }

        if (likedSongs.isNotEmpty()) {
            SectionHeader(
                title = "Favourites",
                count = likedSongs.size,
                onClick = { onFilterChange(LibraryFilter.LIKED) },
            )
            LikedSongsRow(
                songs = likedSongs,
                onSongClick = { song, index ->
                    playerConnection.playQueue(
                        ListQueue(
                            title = "Favourites",
                            items = likedSongs.map { it.toMediaItem() },
                            startIndex = index,
                        ),
                    )
                },
            )
        }

        if (albums.isNotEmpty()) {
            SectionHeader(
                title = "Albums",
                count = albums.size,
                onClick = { onFilterChange(LibraryFilter.ALBUMS) },
            )
            AlbumsRow(albums = albums.take(15), navController = navController)
        }

        if (artists.isNotEmpty()) {
            SectionHeader(
                title = "Artists",
                count = artists.size,
                onClick = { onFilterChange(LibraryFilter.ARTISTS) },
            )
            ArtistsRow(artists = artists.take(15), navController = navController)
        }
    }
}

@Composable
private fun OfflineHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrokenIcon(
                codePoint = BrokenIcons.arrowLeft,
                contentDescription = "Back",
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Offline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyOfflineView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrokenIcon(
                codePoint = BrokenIcons.musicCircle,
                contentDescription = null,
                size = 64.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No offline content yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Download songs or add to favourites\nto see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "• $count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "See all",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
private fun DownloadsRow(
    songs: List<Song>,
    onSongClick: (Song, Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(songs.take(15), key = { it.id }) { song ->
            SongCard(
                song = song,
                onClick = { onSongClick(song, songs.indexOf(song)) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LikedSongsRow(
    songs: List<Song>,
    onSongClick: (Song, Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(songs.take(15), key = { it.id }) { song ->
            SongCard(
                song = song,
                onClick = { onSongClick(song, songs.indexOf(song)) },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SongCard(
    song: Song,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        ) {
            if (song.thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    BrokenIcon(
                        codePoint = BrokenIcons.musicnote,
                        contentDescription = null,
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = song.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.artists?.joinToString { it.name } ?: "Unknown",
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumsRow(
    albums: List<Album>,
    navController: NavController,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums.take(15), key = { it.id }) { album ->
            Card(
                modifier = Modifier
                    .width(140.dp)
                    .clickable { navController.navigate("album/${album.id}") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                ) {
                    if (album.thumbnailUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            BrokenIcon(
                                codePoint = BrokenIcons.musicDashboard,
                                contentDescription = null,
                                size = 32.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        text = album.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = (album.artists.firstOrNull()?.name ?: "Unknown"),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ArtistsRow(
    artists: List<Artist>,
    navController: NavController,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(artists.take(15), key = { it.id }) { artist ->
            Column(
                modifier = Modifier
                    .width(90.dp)
                    .clickable { navController.navigate("artist/${artist.id}") },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                ) {
                    if (artist.thumbnailUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artist.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            BrokenIcon(
                                codePoint = BrokenIcons.profileCircle,
                                contentDescription = null,
                                size = 28.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = artist.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}
