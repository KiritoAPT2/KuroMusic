package com.kuromusic.viewmodels

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuromusic.db.entities.AlbumEntity
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.db.entities.Song
import com.kuromusic.db.entities.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocalSongsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs = _localSongs.asStateFlow()

    init {
        loadLocalSongs()
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            _localSongs.value = getLocalAudio()
        }
    }

    private suspend fun getLocalAudio(): List<Song> = withContext(Dispatchers.IO) {
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
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "${MediaStore.Audio.Media.DURATION} >= 45000 AND (" +
                "${MediaStore.Audio.Media.MIME_TYPE} = 'audio/mpeg' OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} = 'audio/mp4' OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} = 'audio/x-m4a')"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
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
                        id = contentUri, // Use content URI as ID for player
                        title = title,
                        duration = duration / 1000,
                        thumbnailUrl = albumArtUri,
                        albumId = albumId.toString(),
                        albumName = albumName
                    )

                    val artistEntity = ArtistEntity(
                        id = artist, // Dummy ID
                        name = artist
                    )
                    
                    val albumEntity = AlbumEntity(
                        id = albumId.toString(),
                        title = albumName,
                        songCount = 0, // Placeholder
                        duration = 0 // Placeholder
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
        return@withContext songs
    }
}
