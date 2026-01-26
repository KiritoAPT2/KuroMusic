package com.kuromusic.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kuromusic.R
import com.kuromusic.constants.ChipSortTypeKey
import com.kuromusic.constants.LibraryFilter
import com.kuromusic.ui.component.ChipsRow
import com.kuromusic.ui.component.VerticalFastScroller
import com.kuromusic.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val lazyListState = rememberLazyListState()

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips =
                    listOf(
                        LibraryFilter.DOWNLOADS to stringResource(R.string.filter_downloads), // Need R.string.filter_downloads or similar
                        LibraryFilter.LOCAL to "Local",
                        LibraryFilter.CACHED to "En caché", // Hardcoded for now if string missing, or use existing generic
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            when (filterType) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                LibraryFilter.SONGS -> LibrarySongsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.DOWNLOADS -> LibrarySongsScreen(
                    navController = navController,
                    onDeselect = { filterType = LibraryFilter.LIBRARY },
                    initialFilter = com.kuromusic.constants.SongFilter.DOWNLOADED
                )

                LibraryFilter.CACHED -> LibrarySongsScreen(
                    navController = navController,
                    onDeselect = { filterType = LibraryFilter.LIBRARY },
                    initialFilter = com.kuromusic.constants.SongFilter.DOWNLOADED // Mapping Cached -> Downloaded
                )
                
                LibraryFilter.LIKED -> LibrarySongsScreen(
                    navController = navController,
                    onDeselect = { filterType = LibraryFilter.LIBRARY },
                    initialFilter = com.kuromusic.constants.SongFilter.LIKED
                )
                
                LibraryFilter.LOCAL -> LibraryLocalSongsScreen(
                    navController = navController,
                    onDeselect = { filterType = LibraryFilter.LIBRARY }
                )
            }
        }
    }
}
