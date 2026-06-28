package com.kuromusic.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import com.kuromusic.constants.ArtistSortType
import com.kuromusic.db.entities.AlbumArtistMap
import com.kuromusic.db.entities.Artist
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.extensions.reversed
import com.kuromusic.innertube.pages.ArtistPage
import com.kuromusic.ui.utils.resize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale

@Dao
@SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
interface ArtistDao {
    @Transaction
    @Query(
        """
        SELECT DISTINCT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id) AS songCount
        FROM artist
                 LEFT JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC) AS artistTotalPlayTime
                     ON artist.id = artistId
                     OR artist.bookmarkedAt IS NOT NULL
                     ORDER BY 
                      CASE 
                        WHEN artistTotalPlayTime.artistId IS NULL THEN 1 
                        ELSE 0 
                      END, 
                      artistTotalPlayTime.totalPlayTime DESC
    """,
    )
    fun allArtistsByPlayTime(): Flow<List<Artist>>

    @Transaction
    @Query(
        """
        SELECT *,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
        WHERE (songCount > 0 OR bookmarkedAt IS NOT NULL)
        ORDER BY rowId DESC
        """
    )
    fun artistsInAA(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY rowId",
    )
    fun artistsByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY name",
    )
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT * FROM artist WHERE songCount > 0 ORDER BY songCount"
    )
    fun artistsBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE songCount > 0
    """,
    )
    fun artistsByPlayTimeAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE (bookmarkedAt IS NOT NULL OR id IN (SELECT artistId FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE song.liked = 1)) ORDER BY bookmarkedAt",
    )
    fun artistsBookmarkedByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE (bookmarkedAt IS NOT NULL OR id IN (SELECT artistId FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE song.liked = 1)) ORDER BY name",
    )
    fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE (bookmarkedAt IS NOT NULL OR id IN (SELECT artistId FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE song.liked = 1)) ORDER BY songCount",
    )
    fun artistsBookmarkedBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE (bookmarkedAt IS NOT NULL OR artist.id IN (SELECT artistId FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE song.liked = 1))
    """,
    )
    fun artistsBookmarkedByPlayTimeAsc(): Flow<List<Artist>>

    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Query("SELECT * FROM artist WHERE id = :id")
    fun artistById(id: String): ArtistEntity?

    @Transaction
    @Query("SELECT * FROM artist WHERE name = :name")
    fun artistByName(name: String): ArtistEntity?

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE name LIKE '%' || :query || '%' AND songCount > 0 LIMIT :previewSize",
    )
    fun searchArtists(query: String, previewSize: Int = Int.MAX_VALUE): Flow<List<Artist>>

    @Query("UPDATE artist SET songCount = :count WHERE id = :artistId")
    suspend fun setArtistSongCount(artistId: String, count: Int)

    @Query("SELECT COUNT(*) FROM song_artist_map WHERE artistId = :artistId")
    suspend fun getSongCountForArtist(artistId: String): Int

    @Query("SELECT * FROM artist WHERE thumbnailUrl IS NULL")
    suspend fun getArtistsWithoutThumbnails(): List<ArtistEntity>

    @Query("UPDATE artist SET thumbnailUrl = :thumbnailUrl WHERE id = :id")
    suspend fun updateArtistThumbnail(id: String, thumbnailUrl: String)

    // ─── CRUD ─────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(artist: ArtistEntity)

    @Update
    fun update(artist: ArtistEntity)

    @Delete
    fun delete(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: AlbumArtistMap)

    @Delete
    fun delete(albumArtistMap: AlbumArtistMap)

    @Transaction
    suspend fun refreshArtistSongCount(artistId: String) {
        val count = getSongCountForArtist(artistId)
        setArtistSongCount(artistId, count)
    }

    fun upsertArtist(artist: ArtistEntity, artistPage: ArtistPage) {
        update(
            artist.copy(
                name = artistPage.artist.title,
                thumbnailUrl = artistPage.artist.thumbnail.resize(544, 544),
                lastUpdateTime = LocalDateTime.now(),
            ),
        )
    }

    // ─── Sorting facades ──────────────────────────────────────────────────────
    fun artists(
        sortType: ArtistSortType,
        descending: Boolean,
    ) = when (sortType) {
        ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
        ArtistSortType.NAME ->
            artistsByNameAsc().map { artist ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                artist.sortedWith(compareBy(collator) { it.artist.name })
            }

        ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
        ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
    }.map { artists ->
        artists
            .filter { it.artist.isYouTubeArtist }
            .reversed(descending)
    }

    fun artistsBookmarked(
        sortType: ArtistSortType,
        descending: Boolean,
    ) = when (sortType) {
        ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
        ArtistSortType.NAME ->
            artistsBookmarkedByNameAsc().map { artist ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                artist.sortedWith(compareBy(collator) { it.artist.name })
            }

        ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
        ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
    }.map { artists ->
        artists
            .filter { it.artist.isYouTubeArtist }
            .reversed(descending)
    }
}
