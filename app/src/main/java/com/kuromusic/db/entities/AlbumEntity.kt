package com.kuromusic.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuromusic.sync.YouTubeActionHandler
import java.time.LocalDateTime

@Immutable
@Entity(
    tableName = "album",
    indices = [
        androidx.room.Index(value = ["bookmarkedAt"]),
        androidx.room.Index(value = ["inLibrary"]),
        androidx.room.Index(value = ["lastUpdateTime"])
    ]
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val playlistId: String? = null,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val themeColor: Int? = null,
    val songCount: Int,
    val duration: Int,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
) {
    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    fun toggleLike() = localToggleLike().also {
        if (playlistId != null)
            YouTubeActionHandler.likePlaylist(playlistId, it.bookmarkedAt != null)
    }
}
