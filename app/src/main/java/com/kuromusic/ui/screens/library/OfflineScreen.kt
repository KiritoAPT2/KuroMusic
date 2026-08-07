package com.kuromusic.ui.screens.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.kuromusic.LocalDatabase
import com.kuromusic.LocalDownloadUtil
import com.kuromusic.LocalPlayerAwareWindowInsets
import com.kuromusic.LocalPlayerConnection
import com.kuromusic.R
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.db.entities.Song
import com.kuromusic.db.entities.SongEntity
import com.kuromusic.extensions.toMediaItem
import com.kuromusic.extensions.togglePlayPause
import com.kuromusic.playback.RealDownloader
import com.kuromusic.playback.queues.ListQueue
import com.kuromusic.ui.component.LocalMenuState
import com.kuromusic.ui.component.SongListItem
import com.kuromusic.ui.component.VerticalFastScroller
import com.kuromusic.ui.icons.BrokenIcon
import com.kuromusic.ui.icons.BrokenIcons
import com.kuromusic.ui.menu.SongMenu
import com.kuromusic.ui.screens.Screens
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.flowOf

@Composable
fun OfflineScreen(navController: NavController) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    LaunchedEffect(Unit) {
        downloadUtil.restoreDownloadStates()
    }

    val localFiles = remember { RealDownloader.listLocalAudioFiles(context) }

    val appSongIds = remember(localFiles) {
        localFiles.mapNotNull { it.songId }.toSet()
    }

    val dbSongs by remember(database, appSongIds) {
        if (appSongIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            database.songsByIds(appSongIds.toList())
        }
    }.collectAsState(initial = emptyList())

    val allSongs = remember(dbSongs, localFiles) {
        val dbIds = dbSongs.map { it.id }.toSet()
        val localSongs = localFiles
            .filter { it.songId == null || it.songId !in dbIds }
            .map { file ->
                Song(
                    song = SongEntity(
                        id = file.uri.toString(),
                        title = file.title,
                        duration = if (file.durationMs > 0) (file.durationMs / 1000).toInt() else -1,
                        thumbnailUrl = file.thumbnailUri?.toString()
                            ?: file.songId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" },
                    ),
                    artists = if (file.artist != "Unknown Artist") {
                        listOf(ArtistEntity(id = ArtistEntity.generateArtistId(), name = file.artist))
                    } else emptyList(),
                )
            }
        android.util.Log.d("OfflineScreen", "dbSongs: ${dbSongs.map { "${it.id} -> ${it.artists.joinToString { a -> a.name }}" }}")
        android.util.Log.d("OfflineScreen", "appSongIds: $appSongIds")
        android.util.Log.d("OfflineScreen", "localFiles: ${localFiles.map { "${it.songId} -> ${it.artist}" }}")
        dbSongs + localSongs
    }

    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                item(key = "header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        IconButton(onClick = {
                            navController.navigate(Screens.Home.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }) {
                            BrokenIcon(
                                codePoint = BrokenIcons.arrowLeft,
                                contentDescription = "Back",
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = pluralStringResource(R.plurals.n_song, allSongs.size, allSongs.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                itemsIndexed(
                    items = allSongs,
                    key = { _, song -> song.id },
                ) { index, song ->
                    SongListItem(
                        song = song,
                        showInLibraryIcon = false,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Offline",
                                                items = allSongs.map { it.toMediaItem() },
                                                startIndex = index,
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                            .animateItem(),
                    )
                }
            }
        }
    }
}
