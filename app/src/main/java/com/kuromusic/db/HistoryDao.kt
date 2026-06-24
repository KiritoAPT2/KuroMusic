package com.kuromusic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kuromusic.db.entities.Event
import com.kuromusic.db.entities.EventWithSong
import com.kuromusic.db.entities.SearchHistory
import com.kuromusic.db.entities.Song
import com.kuromusic.db.entities.SongHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(songHistory: SongHistory)

    @Transaction
    @Query("SELECT song.* FROM song JOIN song_history ON song.id = song_history.songId ORDER BY song_history.timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 10): Flow<List<Song>>

    @Query("DELETE FROM song_history")
    suspend fun clearSongHistory()

    @Query("DELETE FROM song_history WHERE songId = :songId")
    suspend fun deleteSongHistory(songId: String)

    @Query("DELETE FROM song_history WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteSongHistoryOlderThan(cutoffTimestamp: Long)

    @Transaction
    @Query("DELETE FROM song_history WHERE songId NOT IN (SELECT id FROM song)")
    suspend fun deleteHistoryOrphaned()

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId ASC LIMIT 1")
    fun firstEvent(): Flow<EventWithSong>

    @Transaction
    @Query("DELETE FROM event")
    fun clearListenHistory()

    @Transaction
    @Query("SELECT * FROM search_history WHERE `query` LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchHistory(query: String = ""): Flow<List<SearchHistory>>

    @Transaction
    @Query("DELETE FROM search_history")
    fun clearSearchHistory()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchHistory: SearchHistory)
}
