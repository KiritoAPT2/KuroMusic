package com.kuromusic.viewmodels

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.AlbumEntity
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.db.entities.LocalItem
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.Song
import com.kuromusic.db.entities.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    private fun searchLocalAudio(query: String): Flow<List<Song>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM
        )
        // Strict filters + Search query
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND " +
                "${MediaStore.Audio.Media.DURATION} >= 15000 AND (" +
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)"

        val searchArg = "%$query%"
        val selectionArgs = arrayOf(searchArg, searchArg)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getInt(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val albumName = cursor.getString(albumColumn) ?: "Unknown Album"

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                    val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()

                    val songEntity = SongEntity(
                        id = contentUri,
                        title = title,
                        duration = duration / 1000,
                        thumbnailUrl = albumArtUri,
                        albumId = albumId.toString(),
                        albumName = albumName
                    )

                    val artistEntity = ArtistEntity(
                        id = artist,
                        name = artist
                    )

                    val albumEntity = AlbumEntity(
                        id = albumId.toString(),
                        title = albumName,
                        songCount = 0,
                        duration = 0
                    )

                    songs.add(Song(
                        song = songEntity,
                        artists = listOf(artistEntity),
                        album = albumEntity
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emit(songs)
    }.flowOn(Dispatchers.IO)

@OptIn(kotlinx.coroutines.FlowPreview::class)
    val result =
        combine(
            query.debounce(300).distinctUntilChanged(),
            filter
        ) { query, filter ->
            query to filter
        }.flatMapLatest { (query, filter) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            database.songDao.searchSongs(query, PREVIEW_SIZE),
                            searchLocalAudio(query),
                            database.albumDao.searchAlbums(query, PREVIEW_SIZE),
                            database.artistDao.searchArtists(query, PREVIEW_SIZE),
                            database.playlistDao.searchPlaylists(query, PREVIEW_SIZE),
                        ) { dbSongs, localSongs, albums, artists, playlists ->
                            (dbSongs + localSongs).distinctBy { it.id } + albums + artists + playlists
                        }

                    LocalFilter.SONG -> combine(
                        database.songDao.searchSongs(query),
                        searchLocalAudio(query)
                    ) { dbSongs, localSongs ->
                        (dbSongs + localSongs).distinctBy { it.id }
                    }
                    LocalFilter.ALBUM -> database.albumDao.searchAlbums(query)
                    LocalFilter.ARTIST -> database.artistDao.searchArtists(query)
                    LocalFilter.PLAYLIST -> database.playlistDao.searchPlaylists(query)
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                        list.groupBy {
                            when (it) {
                                is Song -> LocalFilter.SONG
                                is Album -> LocalFilter.ALBUM
                                is Artist -> LocalFilter.ARTIST
                                is Playlist -> LocalFilter.PLAYLIST
                                else -> LocalFilter.ALL // Should not happen
                            }
                        },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 8
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
