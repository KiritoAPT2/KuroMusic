package com.kuromusic.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.kuromusic.constants.AlbumSortType
import com.kuromusic.constants.ArtistSongSortType
import com.kuromusic.constants.ArtistSortType
import com.kuromusic.constants.PlaylistSortType
import com.kuromusic.constants.SongSortType
import com.kuromusic.db.entities.Album
import com.kuromusic.db.entities.AlbumArtistMap
import com.kuromusic.db.entities.AlbumEntity
import com.kuromusic.db.entities.AlbumWithSongs
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.db.entities.Event
import com.kuromusic.db.entities.EventWithSong
import com.kuromusic.db.entities.FormatEntity
import com.kuromusic.db.entities.LyricsEntity
import com.kuromusic.db.entities.PlayCountEntity
import com.kuromusic.db.entities.Playlist
import com.kuromusic.db.entities.PlaylistEntity
import com.kuromusic.db.entities.PlaylistSong
import com.kuromusic.db.entities.PlaylistSongMap
import com.kuromusic.db.entities.PlaylistSongMapPreview
import com.kuromusic.db.entities.RelatedSongMap
import com.kuromusic.db.entities.SearchHistory
import com.kuromusic.db.entities.SetVideoIdEntity
import com.kuromusic.db.entities.Song
import com.kuromusic.db.entities.SongAlbumMap
import com.kuromusic.db.entities.SongArtistMap
import com.kuromusic.db.entities.SongEntity
import com.kuromusic.db.entities.SongHistory
import com.kuromusic.db.entities.SongWithStats
import com.kuromusic.db.entities.SortedSongAlbumMap
import com.kuromusic.db.entities.SortedSongArtistMap
import com.kuromusic.extensions.toSQLiteQuery
import com.kuromusic.innertube.models.PlaylistItem as InnerTubePlaylistItem
import com.kuromusic.innertube.models.SongItem
import com.kuromusic.innertube.pages.AlbumPage
import com.kuromusic.innertube.pages.ArtistPage
import com.kuromusic.models.MediaMetadata
import com.kuromusic.models.toMediaMetadata
import com.kuromusic.ui.utils.resize
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

class MusicDatabase(
    private val delegate: InternalDatabase,
) {
    val songDao: SongDao by delegate::songDao
    val artistDao: ArtistDao by delegate::artistDao
    val albumDao: AlbumDao by delegate::albumDao
    val playlistDao: PlaylistDao by delegate::playlistDao
    val historyDao: HistoryDao by delegate::historyDao
    val statsDao: StatsDao by delegate::statsDao
    val openHelper: SupportSQLiteOpenHelper
        get() = delegate.openHelper

    fun query(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            queryExecutor.execute {
                block(this@MusicDatabase)
            }
        }

    fun transaction(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            transactionExecutor.execute {
                runInTransaction {
                    block(this@MusicDatabase)
                }
            }
        }

    fun close() = delegate.close()

    // ═══════════════════════════════════════════════════════════════════════════
    // SongDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun songs(sortType: SongSortType, descending: Boolean) =
        songDao.songs(sortType, descending)

    fun likedSongs(sortType: SongSortType, descending: Boolean) =
        songDao.likedSongs(sortType, descending)

    fun artistSongs(artistId: String, sortType: ArtistSongSortType, descending: Boolean) =
        songDao.artistSongs(artistId, sortType, descending)

    fun albumSongs(albumId: String) = songDao.albumSongs(albumId)
    fun playlistSongs(playlistId: String) = songDao.playlistSongs(playlistId)
    fun allSongs() = songDao.allSongs()
    fun song(songId: String?) = songDao.song(songId)
    fun getSongById(songId: String) = songDao.getSongById(songId)
    fun recentlyPlayedSongs(limit: Int = 20) = songDao.recentlyPlayedSongs(limit)
    fun essentialSongs(limit: Int = 20) = songDao.essentialSongs(limit)
    fun quickPicks(now: Long = System.currentTimeMillis()) = songDao.quickPicks(now)
    fun forgottenFavorites(now: Long = System.currentTimeMillis()) = songDao.forgottenFavorites(now)
    fun recommendedAlbum(now: Long = System.currentTimeMillis(), limit: Int = 5, offset: Int = 0) =
        songDao.recommendedAlbum(now, limit, offset)
    fun searchSongs(query: String, previewSize: Int = Int.MAX_VALUE) =
        songDao.searchSongs(query, previewSize)
    fun topRecentArtist(since: Long = System.currentTimeMillis() - 604800000) =
        songDao.topRecentArtist(since)
    fun topRecentGenre(since: Long = System.currentTimeMillis() - 604800000) =
        songDao.topRecentGenre(since)
    fun hasRelatedSongs(songId: String) = songDao.hasRelatedSongs(songId)
    fun getRelatedSongs(songId: String) = songDao.getRelatedSongs(songId)
    fun relatedSongs(songId: String) = songDao.relatedSongs(songId)
    fun artistSongsPreview(artistId: String, previewSize: Int = 3) =
        songDao.artistSongsPreview(artistId, previewSize)

    // Song CRUD
    fun insert(song: SongEntity) = songDao.insert(song)
    fun upsert(song: SongEntity) = songDao.upsert(song)
    fun update(song: SongEntity) = songDao.update(song)
    fun delete(song: SongEntity) = songDao.delete(song)
    fun insert(map: SongArtistMap) = songDao.insert(map)
    fun delete(map: SongArtistMap) = songDao.delete(map)
    fun insert(map: SongAlbumMap) = songDao.insert(map)
    fun upsert(map: SongAlbumMap) = songDao.upsert(map)
    fun insert(map: RelatedSongMap) = songDao.insert(map)
    fun upsert(lyrics: LyricsEntity) = songDao.upsert(lyrics)
    fun delete(lyrics: LyricsEntity) = songDao.delete(lyrics)
    fun upsert(format: FormatEntity) = songDao.upsert(format)
    suspend fun insertSetVideoId(entity: SetVideoIdEntity) = songDao.insertSetVideoId(entity)
    suspend fun getSetVideoId(videoId: String) = songDao.getSetVideoId(videoId)
    fun format(id: String?) = songDao.format(id)
    suspend fun getLyrics(id: String?) = songDao.getLyrics(id)
    fun lyrics(id: String?) = songDao.lyrics(id)
    fun inLibrary(songId: String, inLibrary: LocalDateTime?) =
        songDao.inLibrary(songId, inLibrary)
    fun songArtistMap(songId: String) = songDao.songArtistMap(songId)

    // ═══════════════════════════════════════════════════════════════════════════
    // ArtistDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun artists(sortType: ArtistSortType, descending: Boolean) =
        artistDao.artists(sortType, descending)

    fun artistsBookmarked(sortType: ArtistSortType, descending: Boolean) =
        artistDao.artistsBookmarked(sortType, descending)

    fun artist(id: String) = artistDao.artist(id)
    fun artistByName(name: String) = artistDao.artistByName(name)
    fun searchArtists(query: String, previewSize: Int = Int.MAX_VALUE) =
        artistDao.searchArtists(query, previewSize)
    fun allArtistsByPlayTime() = artistDao.allArtistsByPlayTime()
    fun artistsInAA() = artistDao.artistsInAA()
    suspend fun setArtistSongCount(artistId: String, count: Int) =
        artistDao.setArtistSongCount(artistId, count)
    suspend fun getSongCountForArtist(artistId: String) =
        artistDao.getSongCountForArtist(artistId)
    suspend fun getArtistsWithoutThumbnails() = artistDao.getArtistsWithoutThumbnails()
    suspend fun updateArtistThumbnail(id: String, thumbnailUrl: String) =
        artistDao.updateArtistThumbnail(id, thumbnailUrl)

    // Artist CRUD
    fun insert(artist: ArtistEntity) = artistDao.insert(artist)
    fun upsert(artist: ArtistEntity) = artistDao.upsert(artist)
    fun update(artist: ArtistEntity) = artistDao.update(artist)
    fun delete(artist: ArtistEntity) = artistDao.delete(artist)
    fun insert(map: AlbumArtistMap) = artistDao.insert(map)
    fun delete(map: AlbumArtistMap) = artistDao.delete(map)

    suspend fun refreshArtistSongCount(artistId: String) =
        artistDao.refreshArtistSongCount(artistId)

    // ═══════════════════════════════════════════════════════════════════════════
    // AlbumDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun albums(sortType: AlbumSortType, descending: Boolean) =
        albumDao.albums(sortType, descending)

    fun albumsLiked(sortType: AlbumSortType, descending: Boolean) =
        albumDao.albumsLiked(sortType, descending)

    fun album(id: String) = albumDao.album(id)
    fun albumWithSongs(albumId: String) = albumDao.albumWithSongs(albumId)
    fun albumArtistMaps(albumId: String) = albumDao.albumArtistMaps(albumId)
    fun searchAlbums(query: String, previewSize: Int = Int.MAX_VALUE) =
        albumDao.searchAlbums(query, previewSize)
    fun albumsInAA() = albumDao.albumsInAA()

    // Album CRUD
    fun insert(album: AlbumEntity) = albumDao.insert(album)
    fun update(album: AlbumEntity) = albumDao.update(album)
    fun delete(album: AlbumEntity) = albumDao.delete(album)

    // ═══════════════════════════════════════════════════════════════════════════
    // PlaylistDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun playlists(sortType: PlaylistSortType, descending: Boolean) =
        playlistDao.playlists(sortType, descending)

    fun playlist(playlistId: String) = playlistDao.playlist(playlistId)
    fun playlistByBrowseId(browseId: String) = playlistDao.playlistByBrowseId(browseId)
    fun searchPlaylists(query: String, previewSize: Int = Int.MAX_VALUE) =
        playlistDao.searchPlaylists(query, previewSize)
    fun editablePlaylistsByCreateDateAsc() = playlistDao.editablePlaylistsByCreateDateAsc()
    fun checkInPlaylist(playlistId: String, songId: String) =
        playlistDao.checkInPlaylist(playlistId, songId)
    fun playlistDuplicates(playlistId: String, songIds: List<String>) =
        playlistDao.playlistDuplicates(playlistId, songIds)
    fun playlistSongMaps(songId: String) = playlistDao.playlistSongMaps(songId)
    fun playlistSongMapsFrom(playlistId: String, from: Int) =
        playlistDao.playlistSongMapsFrom(playlistId, from)
    fun addSongToPlaylist(playlist: Playlist, songIds: List<String>) =
        playlistDao.addSongToPlaylist(playlist, songIds)

    // Playlist CRUD
    fun insert(playlist: PlaylistEntity) = playlistDao.insert(playlist)
    fun update(playlist: PlaylistEntity) = playlistDao.update(playlist)
    fun delete(playlist: PlaylistEntity) = playlistDao.delete(playlist)
    fun insert(map: PlaylistSongMap) = playlistDao.insert(map)
    fun update(map: PlaylistSongMap) = playlistDao.update(map)
    fun delete(map: PlaylistSongMap) = playlistDao.delete(map)
    fun move(playlistId: String, fromPosition: Int, toPosition: Int) =
        playlistDao.move(playlistId, fromPosition, toPosition)
    fun clearPlaylist(playlistId: String) = playlistDao.clearPlaylist(playlistId)
    fun deletePlaylistById(browseId: String) = playlistDao.deletePlaylistById(browseId)

    // ═══════════════════════════════════════════════════════════════════════════
    // HistoryDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun events() = historyDao.events()
    fun firstEvent() = historyDao.firstEvent()
    fun clearListenHistory() = historyDao.clearListenHistory()
    fun searchHistory(query: String = "") = historyDao.searchHistory(query)
    fun clearSearchHistory() = historyDao.clearSearchHistory()
    fun getRecentHistory(limit: Int = 10) = historyDao.getRecentHistory(limit)
    suspend fun clearSongHistory() = historyDao.clearSongHistory()
    suspend fun deleteSongHistory(songId: String) = historyDao.deleteSongHistory(songId)
    suspend fun deleteSongHistoryOlderThan(cutoffTimestamp: Long) =
        historyDao.deleteSongHistoryOlderThan(cutoffTimestamp)
    suspend fun deleteHistoryOrphaned() = historyDao.deleteHistoryOrphaned()

    // History CRUD
    suspend fun insert(songHistory: SongHistory) = historyDao.insert(songHistory)
    fun insert(event: Event) = historyDao.insert(event)
    fun insert(searchHistory: SearchHistory) = historyDao.insert(searchHistory)
    fun delete(searchHistory: SearchHistory) = historyDao.delete(searchHistory)
    fun delete(event: Event) = historyDao.delete(event)

    // ═══════════════════════════════════════════════════════════════════════════
    // StatsDao delegation
    // ═══════════════════════════════════════════════════════════════════════════
    fun mostPlayedSongsStats(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = java.time.LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
    ) = statsDao.mostPlayedSongsStats(fromTimeStamp, limit, offset, toTimeStamp)
    fun mostPlayedSongs(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = java.time.LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
    ) = statsDao.mostPlayedSongs(fromTimeStamp, limit, offset, toTimeStamp)
    fun mostPlayedArtists(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = java.time.LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
    ) = statsDao.mostPlayedArtists(fromTimeStamp, limit, offset, toTimeStamp)
    fun mostPlayedAlbums(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = java.time.LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
    ) = statsDao.mostPlayedAlbums(fromTimeStamp, limit, offset, toTimeStamp)
    fun getLifetimePlayCount(songId: String?) = statsDao.getLifetimePlayCount(songId)
    fun getPlayCountByYear(songId: String?, year: Int) = statsDao.getPlayCountByYear(songId, year)
    fun getPlayCountByMonth(songId: String?, year: Int, month: Int) =
        statsDao.getPlayCountByMonth(songId, year, month)
    fun eventCount() = statsDao.eventCount()
    fun getTopGenres(limit: Int = 3) = statsDao.getTopGenres(limit)
    fun getRecentPlayedIds(limit: Int = 100) = statsDao.getRecentPlayedIds(limit)
    fun getSongsFromGenres(genres: List<String>, excludeIds: List<String>, limit: Int = 30) =
        statsDao.getSongsFromGenres(genres, excludeIds, limit)
    fun getUnplayedSongsFromGenres(genres: List<String>, limit: Int = 15) =
        statsDao.getUnplayedSongsFromGenres(genres, limit)
    fun incrementTotalPlayTime(songId: String, playTime: Long) =
        statsDao.incrementTotalPlayTime(songId, playTime)
    fun incrementPlayCount(songId: String, year: Int, month: Int) =
        statsDao.incrementPlayCount(songId, year, month)
    fun incrementPlayCount(songId: String) = statsDao.incrementPlayCount(songId)
    fun insert(playCountEntity: PlayCountEntity) = statsDao.insert(playCountEntity)

    // ═══════════════════════════════════════════════════════════════════════════
    // Complex multi-DAO operations (originally in DatabaseDao)
    // ═══════════════════════════════════════════════════════════════════════════
    fun checkpoint() {
        delegate.queryExecutor.execute {
            delegate.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        }
    }

    @androidx.room.Transaction
    fun insert(mediaMetadata: MediaMetadata, block: (SongEntity) -> SongEntity = { it }) {
        if (songDao.insert(mediaMetadata.toSongEntity().let(block)) == -1L) return
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistDao.artistByName(artist.name)?.id
                ?: ArtistEntity.generateArtistId()
            artistDao.insert(
                ArtistEntity(id = artistId, name = artist.name),
            )
            songDao.insert(
                SongArtistMap(songId = mediaMetadata.id, artistId = artistId, position = index),
            )
        }
    }

    @androidx.room.Transaction
    fun upsert(mediaMetadata: MediaMetadata, block: (SongEntity) -> SongEntity = { it }) {
        val existingSong = songDao.getSongById(mediaMetadata.id)
        if (existingSong != null) {
            songDao.update(
                block(existingSong.song.copy(
                    title = mediaMetadata.title,
                    duration = mediaMetadata.duration,
                    thumbnailUrl = mediaMetadata.thumbnailUrl,
                    albumId = mediaMetadata.album?.id,
                    albumName = mediaMetadata.album?.title,
                )),
            )
        } else {
            songDao.upsert(mediaMetadata.toSongEntity().let(block))
        }
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistDao.artistByName(artist.name)?.id
                ?: ArtistEntity.generateArtistId()
            val existingArtist = artistDao.artistById(artistId)
            if (existingArtist != null) {
                artistDao.update(
                    existingArtist.copy(name = artist.name, lastUpdateTime = LocalDateTime.now()),
                )
            } else {
                artistDao.upsert(ArtistEntity(id = artistId, name = artist.name))
            }
            songDao.insert(
                SongArtistMap(songId = mediaMetadata.id, artistId = artistId, position = index),
            )
        }
    }

    @androidx.room.Transaction
    fun insert(albumPage: AlbumPage) {
        if (albumDao.insert(
                com.kuromusic.db.entities.AlbumEntity(
                    id = albumPage.album.browseId,
                    playlistId = albumPage.album.playlistId,
                    title = albumPage.album.title,
                    year = albumPage.album.year,
                    thumbnailUrl = albumPage.album.thumbnail,
                    songCount = albumPage.songs.size,
                    duration = albumPage.songs.sumOf { it.duration ?: 0 },
                ),
            ) == -1L
        ) {
            return
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach { insert(it) }
            .onEach {
                val existingSong = songDao.getSongById(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(songId = song.id, albumId = albumPage.album.browseId, index = index)
            }.forEach { songDao.upsert(it) }
        albumPage.album.artists
            ?.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistDao.artistByName(artist.name)?.id
                        ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }?.onEach { artistDao.insert(it) }
            ?.mapIndexed { index, artist ->
                AlbumArtistMap(albumId = albumPage.album.browseId, artistId = artist.id, order = index)
            }?.forEach { artistDao.insert(it) }
    }

    @androidx.room.Transaction
    fun update(song: Song, mediaMetadata: MediaMetadata) {
        songDao.update(
            song.song.copy(
                title = mediaMetadata.title,
                duration = mediaMetadata.duration,
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumId = mediaMetadata.album?.id,
                albumName = mediaMetadata.album?.title,
            ),
        )
        songDao.songArtistMap(song.id).forEach { songDao.delete(it) }
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistDao.artistByName(artist.name)?.id
                ?: ArtistEntity.generateArtistId()
            artistDao.insert(ArtistEntity(id = artistId, name = artist.name))
            songDao.insert(
                SongArtistMap(songId = song.id, artistId = artistId, position = index),
            )
        }
    }

    @androidx.room.Transaction
    fun update(
        album: AlbumEntity,
        albumPage: AlbumPage,
        artists: List<ArtistEntity>? = emptyList(),
    ) {
        albumDao.update(
            album.copy(
                id = albumPage.album.browseId,
                playlistId = albumPage.album.playlistId,
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { it.duration ?: 0 },
            ),
        )
        if (artists?.size != albumPage.album.artists?.size) {
            artists?.forEach { artistDao.delete(it) }
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach { insert(it) }
            .onEach {
                val existingSong = songDao.getSongById(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(songId = song.id, albumId = albumPage.album.browseId, index = index)
            }.forEach { songDao.upsert(it) }

        albumPage.album.artists?.let { albumArtists ->
            albumDao.albumArtistMaps(album.id).forEach { artistDao.delete(it) }
            albumArtists
                .map { artist ->
                    ArtistEntity(
                        id = artist.id ?: artistDao.artistByName(artist.name)?.id
                            ?: ArtistEntity.generateArtistId(),
                        name = artist.name,
                    )
                }.onEach { artistDao.insert(it) }
                .mapIndexed { index, artist ->
                    AlbumArtistMap(albumId = albumPage.album.browseId, artistId = artist.id, order = index)
                }.forEach { artistDao.insert(it) }
        }
    }

    fun update(playlistEntity: PlaylistEntity, playlistItem: InnerTubePlaylistItem) {
        playlistDao.update(
            playlistEntity.copy(
                name = playlistItem.title,
                browseId = playlistItem.id,
                isEditable = playlistItem.isEditable,
                remoteSongCount = playlistItem.songCountText?.let {
                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                },
                playEndpointParams = playlistItem.playEndpoint?.params,
                shuffleEndpointParams = playlistItem.shuffleEndpoint?.params,
                radioEndpointParams = playlistItem.radioEndpoint?.params,
            ),
        )
    }

    fun upsertArtist(artist: ArtistEntity, artistPage: ArtistPage) =
        artistDao.upsertArtist(artist, artistPage)

    private fun MediaMetadata.toSongEntityFromMedia() = com.kuromusic.db.entities.SongEntity(
        id = id,
        title = title,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        albumId = album?.id,
        albumName = album?.title,
    )
}

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class,
        SongHistory::class
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = 25,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = Migration5To6::class),
        AutoMigration(from = 6, to = 7, spec = Migration6To7::class),
        AutoMigration(from = 7, to = 8, spec = Migration7To8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = Migration9To10::class),
        AutoMigration(from = 10, to = 11, spec = Migration10To11::class),
        AutoMigration(from = 11, to = 12, spec = Migration11To12::class),
        AutoMigration(from = 12, to = 13, spec = Migration12To13::class),
        AutoMigration(from = 13, to = 14, spec = Migration13To14::class),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17, spec = Migration16To17::class),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25) // Performance indices
    ],
)
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val songDao: SongDao
    abstract val artistDao: ArtistDao
    abstract val albumDao: AlbumDao
    abstract val playlistDao: PlaylistDao
    abstract val historyDao: HistoryDao
    abstract val statsDao: StatsDao

    companion object {
        const val DB_NAME = "song.db"

        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun newInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicDatabase(
                    delegate =
                    Room
                        .databaseBuilder(context.applicationContext, InternalDatabase::class.java, DB_NAME)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .addMigrations(MIGRATION_1_2)
                        .fallbackToDestructiveMigration()
                        .build(),
                ).also { INSTANCE = it }
            }
        }
    }
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            data class OldSongEntity(
                val id: String,
                val title: String,
                val duration: Int = -1, // in seconds
                val thumbnailUrl: String? = null,
                val albumId: String? = null,
                val albumName: String? = null,
                val liked: Boolean = false,
                val totalPlayTime: Long = 0, // in milliseconds
                val downloadState: Int = 0,
                val createDate: LocalDateTime = LocalDateTime.now(),
                val modifyDate: LocalDateTime = LocalDateTime.now(),
            )

            val converters = Converters()
            val artistMap = mutableMapOf<Int, String>()
            val artists = mutableListOf<ArtistEntity>()
            database.query("SELECT * FROM artist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = ArtistEntity.generateArtistId()
                    artistMap[oldId] = newId
                    artists.add(
                        ArtistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }

            val playlistMap = mutableMapOf<Int, String>()
            val playlists = mutableListOf<PlaylistEntity>()
            database.query("SELECT * FROM playlist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = PlaylistEntity.generatePlaylistId()
                    playlistMap[oldId] = newId
                    playlists.add(
                        PlaylistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }
            val playlistSongMaps = mutableListOf<PlaylistSongMap>()
            database.query("SELECT * FROM playlist_song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    playlistSongMaps.add(
                        PlaylistSongMap(
                            playlistId = playlistMap[cursor.getInt(1)]!!,
                            songId = cursor.getString(2),
                            position = cursor.getInt(3),
                        ),
                    )
                }
            }
            // ensure we have continuous playlist song position
            playlistSongMaps.sortBy { it.position }
            val playlistSongCount = mutableMapOf<String, Int>()
            playlistSongMaps.map { map ->
                if (map.playlistId !in playlistSongCount) playlistSongCount[map.playlistId] = 0
                map.copy(position = playlistSongCount[map.playlistId]!!).also {
                    playlistSongCount[map.playlistId] = playlistSongCount[map.playlistId]!! + 1
                }
            }
            val songs = mutableListOf<OldSongEntity>()
            val songArtistMaps = mutableListOf<SongArtistMap>()
            database.query("SELECT * FROM song".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val songId = cursor.getString(0)
                    songs.add(
                        OldSongEntity(
                            id = songId,
                            title = cursor.getString(1),
                            duration = cursor.getInt(3),
                            liked = cursor.getInt(4) == 1,
                            createDate = Instant.ofEpochMilli(Date(cursor.getLong(8)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                            modifyDate = Instant.ofEpochMilli(Date(cursor.getLong(9)).time)
                                .atZone(ZoneOffset.UTC).toLocalDateTime(),
                        ),
                    )
                    songArtistMaps.add(
                        SongArtistMap(
                            songId = songId,
                            artistId = artistMap[cursor.getInt(2)]!!,
                            position = 0,
                        ),
                    )
                }
            }
            database.execSQL("DROP TABLE IF EXISTS song")
            database.execSQL("DROP TABLE IF EXISTS artist")
            database.execSQL("DROP TABLE IF EXISTS playlist")
            database.execSQL("DROP TABLE IF EXISTS playlist_song")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `song` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `thumbnailUrl` TEXT, `albumId` TEXT, `albumName` TEXT, `liked` INTEGER NOT NULL, `totalPlayTime` INTEGER NOT NULL, `isTrash` INTEGER NOT NULL, `download_state` INTEGER NOT NULL, `create_date` INTEGER NOT NULL, `modify_date` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `artist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `bannerUrl` TEXT, `description` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `album` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER, `thumbnailUrl` TEXT, `songCount` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT, `authorId` TEXT, `year` INTEGER, `thumbnailUrl` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_artist_map` (`songId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`songId`, `artistId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_songId` ON `song_artist_map` (`songId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_artistId` ON `song_artist_map` (`artistId`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_album_map` (`songId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `index` INTEGER, PRIMARY KEY(`songId`, `albumId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_songId` ON `song_album_map` (`songId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_albumId` ON `song_album_map` (`albumId`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `album_artist_map` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `artistId`), FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_albumId` ON `album_artist_map` (`albumId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_artistId` ON `album_artist_map` (`artistId`)")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist_song_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_playlistId` ON `playlist_song_map` (`playlistId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_songId` ON `playlist_song_map` (`songId`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `download` (`id` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL)",
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            database.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
            database.execSQL(
                "CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position",
            )
            artists.forEach { artist ->
                database.insert(
                    "artist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to artist.id,
                        "name" to artist.name,
                        "createDate" to converters.dateToTimestamp(artist.lastUpdateTime),
                        "lastUpdateTime" to converters.dateToTimestamp(artist.lastUpdateTime),
                    ),
                )
            }
            songs.forEach { song ->
                database.insert(
                    "song",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to song.id,
                        "title" to song.title,
                        "duration" to song.duration,
                        "liked" to song.liked,
                        "totalPlayTime" to song.totalPlayTime,
                        "isTrash" to false,
                        "download_state" to song.downloadState,
                        "create_date" to converters.dateToTimestamp(song.createDate),
                        "modify_date" to converters.dateToTimestamp(song.modifyDate),
                    ),
                )
            }
            songArtistMaps.forEach { songArtistMap ->
                database.insert(
                    "song_artist_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "songId" to songArtistMap.songId,
                        "artistId" to songArtistMap.artistId,
                        "position" to songArtistMap.position,
                    ),
                )
            }
            playlists.forEach { playlist ->
                database.insert(
                    "playlist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to playlist.id,
                        "name" to playlist.name,
                        "createDate" to converters.dateToTimestamp(LocalDateTime.now()),
                        "lastUpdateTime" to converters.dateToTimestamp(LocalDateTime.now()),
                    ),
                )
            }
            playlistSongMaps.forEach { playlistSongMap ->
                database.insert(
                    "playlist_song_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "playlistId" to playlistSongMap.playlistId,
                        "songId" to playlistSongMap.songId,
                        "position" to playlistSongMap.position,
                    ),
                )
            }
        }
    }

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isTrash"),
    DeleteColumn(tableName = "playlist", columnName = "author"),
    DeleteColumn(tableName = "playlist", columnName = "authorId"),
    DeleteColumn(tableName = "playlist", columnName = "year"),
    DeleteColumn(tableName = "playlist", columnName = "thumbnailUrl"),
    DeleteColumn(tableName = "playlist", columnName = "createDate"),
    DeleteColumn(tableName = "playlist", columnName = "lastUpdateTime"),
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "song",
        fromColumnName = "download_state",
        toColumnName = "downloadState"
    ),
    RenameColumn(tableName = "song", fromColumnName = "create_date", toColumnName = "createDate"),
    RenameColumn(tableName = "song", fromColumnName = "modify_date", toColumnName = "modifyDate"),
)
class Migration5To6 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id FROM playlist WHERE id NOT LIKE 'LP%'").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE playlist SET browseID = '${cursor.getString(0)}' WHERE id = '${
                        cursor.getString(
                            0
                        )
                    }'"
                )
            }
        }
    }
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.query("SELECT id, createDate FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                db.execSQL(
                    "UPDATE song SET inLibrary = ${cursor.getLong(1)} WHERE id = '${
                        cursor.getString(
                            0
                        )
                    }'"
                )
            }
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "createDate"),
    DeleteColumn(tableName = "song", columnName = "modifyDate"),
)
class Migration7To8 : AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "download"),
)
class Migration9To10 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "downloadState"),
    DeleteColumn(tableName = "artist", columnName = "bannerUrl"),
    DeleteColumn(tableName = "artist", columnName = "description"),
    DeleteColumn(tableName = "artist", columnName = "createDate"),
)
class Migration10To11 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "album", columnName = "createDate"),
)
class Migration11To12 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE album SET bookmarkedAt = lastUpdateTime")
        db.query("SELECT DISTINCT albumId, albumName FROM song").use { cursor ->
            while (cursor.moveToNext()) {
                val albumId = cursor.getString(0)
                val albumName = cursor.getString(1)
                db.insert(
                    table = "album",
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE,
                    values =
                        contentValuesOf(
                            "id" to albumId,
                            "title" to albumName,
                            "songCount" to 0,
                            "duration" to 0,
                            "lastUpdateTime" to 0,
                        ),
                )
            }
        }
        db.query("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    }
}

class Migration12To13 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
    }
}

class Migration13To14 : AutoMigrationSpec {
    @SuppressLint("Range")
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET createdAt = '${Converters().dateToTimestamp(LocalDateTime.now())}'")
        db.execSQL(
            "UPDATE playlist SET lastUpdateTime = '${
                Converters().dateToTimestamp(
                    LocalDateTime.now()
                )
            }'"
        )
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isLocal"),
    DeleteColumn(tableName = "song", columnName = "localPath"),
    DeleteColumn(tableName = "artist", columnName = "isLocal"),
    DeleteColumn(tableName = "playlist", columnName = "isLocal"),
)
class Migration16To17 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE playlist SET bookmarkedAt = lastUpdateTime")
        db.execSQL("UPDATE playlist SET isEditable = 1 WHERE browseId IS NOT NULL")
    }
}
