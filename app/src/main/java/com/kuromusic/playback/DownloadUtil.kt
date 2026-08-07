package com.kuromusic.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import com.kuromusic.constants.AudioQuality
import com.kuromusic.constants.AudioQualityKey
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.ArtistEntity
import com.kuromusic.db.entities.FormatEntity
import com.kuromusic.db.entities.SongArtistMap
import com.kuromusic.db.entities.SongEntity
import com.kuromusic.utils.YTPlayerUtils
import com.kuromusic.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages song downloads as real files in filesDir/songs/{id}.opus.
 *
 * Public API is intentionally kept compatible with the old Media3 DownloadManager system:
 * - [downloads]: StateFlow<Map<String, Download>> — same type, same state constants
 * - [getDownload]: Flow<Download?> — same signature
 *
 * New methods:
 * - [startDownload] — replaces DownloadService.sendAddDownload()
 * - [removeDownload] — replaces DownloadService.sendRemoveDownload()
 */
@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    companion object {
        private const val TAG = "DownloadUtil"
        /** Maximum concurrent downloads */
        private const val MAX_CONCURRENT = 3
        /** Max retries for transient network errors */
        private const val MAX_RETRIES = 3
        /** Base delay for exponential backoff (1s, 2s, 4s) */
        private const val RETRY_BASE_DELAY_MS = 1_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)

    /** Tracks active download jobs so we can limit concurrency */
    private val activeJobs = mutableMapOf<String, Job>()

    // ── Public API (compatible with old system) ──────────────────────────

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads = _downloads.asStateFlow()

    fun getDownload(songId: String): Flow<Download?> =
        downloads.map { it[songId] }.distinctUntilChanged()

    // ── New public API ──────────────────────────────────────────────────

    /**
     * Starts downloading a song to filesDir/songs/{id}.opus.
     * Replaces: DownloadService.sendAddDownload(context, ExoDownloadService::class.java, ...)
     *
     * @param songId YouTube video ID
     * @param title Song title (for state tracking / notifications)
     */
    fun startDownload(songId: String, title: String) {
        // Check if already downloaded
        if (_downloads.value[songId]?.state == Download.STATE_COMPLETED) {
            Timber.tag(TAG).d("Already downloaded: $songId")
            return
        }

        // Check if already queued/downloading
        val currentState = _downloads.value[songId]?.state
        if (currentState == Download.STATE_QUEUED || currentState == Download.STATE_DOWNLOADING) {
            Timber.tag(TAG).d("Already downloading: $songId")
            return
        }

        // Enforce concurrent download limit
        val activeCount = activeJobs.count { it.value.isActive }
        if (activeCount >= MAX_CONCURRENT) {
            Timber.tag(TAG).w("Max concurrent downloads reached ($MAX_CONCURRENT). Queueing: $songId")
            // Still allow, it will just wait
        }

        val request = DownloadRequest.Builder(songId, songId.toUri())
            .setData(title.toByteArray())
            .build()

        // Set initial state: QUEUED
        updateDownloadState(
            songId = songId,
            request = request,
            state = Download.STATE_QUEUED,
        )

        val job = scope.launch {
            performDownload(songId, title, request)
        }
        activeJobs[songId] = job
    }

    /**
     * Removes a downloaded song (deletes file + clears DB state).
     * Replaces: DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, ...)
     */
    fun removeDownload(songId: String) {
        activeJobs[songId]?.cancel()
        activeJobs.remove(songId)

        RealDownloader.deleteFromMediaStore(context, songId)
        RealDownloader.deleteSongFile(context, songId)

        _downloads.update { it - songId }
        updateDateDownloadInDb(songId, null)

        Timber.tag(TAG).d("Removed download: $songId")
    }

    // ── Internal: actual download logic ─────────────────────────────────

    private suspend fun performDownload(
        songId: String,
        title: String,
        request: DownloadRequest,
    ) {
        Timber.tag(TAG).d("Starting download: $songId - $title")

        var lastError: Exception? = null
        var tempFile: File? = null
        var resolvedStreamUrl: String? = null
        var dbItag: Int? = null
        var dbMimeType: String? = null
        var dbCodecs: String? = null
        var dbBitrate: Int? = null
        var dbSampleRate: Int? = null
        var dbContentLength: Long? = null
        var dbLoudnessDb: Double? = null
        var dbArtist: String? = null
        var dbThumbnailUrl: String? = null
        var dbDuration: Int = -1

        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1))
                Timber.tag(TAG).w("Retry $attempt/$MAX_RETRIES for $songId after ${delayMs}ms...")
                delay(delayMs)
            }

            try {
                if (attempt == 0) {
                    updateDownloadState(songId, request, Download.STATE_DOWNLOADING)
                }

                if (resolvedStreamUrl == null) {
                    val playbackData = YTPlayerUtils.playerResponseForPlayback(
                        videoId = songId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    ).getOrThrow()

                    resolvedStreamUrl = playbackData.streamUrl
                    dbArtist = playbackData.videoDetails?.author
                    dbDuration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: -1
                    dbThumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url
                    Timber.tag(TAG).d("Extracted — artist: $dbArtist, thumbnail: $dbThumbnailUrl, duration: $dbDuration, videoDetails: ${playbackData.videoDetails}")
                    dbMimeType = (playbackData.format.mimeType ?: "audio/webm").split(";")[0]
                    dbItag = playbackData.format.itag
                    dbCodecs = try {
                        (playbackData.format.mimeType ?: "").split("codecs=")[1].removeSurrounding("\"")
                    } catch (_: Exception) { "opus" }
                    dbBitrate = playbackData.format.bitrate
                    dbSampleRate = playbackData.format.audioSampleRate
                    dbContentLength = playbackData.format.contentLength
                    dbLoudnessDb = playbackData.audioConfig?.loudnessDb

                    tempFile = RealDownloader.downloadStreamToTemp(
                        url = resolvedStreamUrl!!,
                        songId = songId,
                        mimeType = dbMimeType ?: "audio/webm",
                        context = context,
                        progress = { downloaded, total ->
                            val percent = if (total > 0) (downloaded * 100 / total).toInt() else 0
                            updateDownloadState(
                                songId, request, Download.STATE_DOWNLOADING,
                                percentDownloaded = percent,
                                bytesDownloaded = downloaded,
                                contentLength = total,
                            )
                        },
                    )
                }

                RealDownloader.saveToMediaStore(
                    context = context,
                    songId = songId,
                    title = title,
                    artist = dbArtist,
                    mimeType = dbMimeType ?: "audio/webm",
                    sourceFile = tempFile!!,
                )

                if (dbThumbnailUrl != null) {
                    try {
                        val thumbBytes = RealDownloader.downloadImageBytes(dbThumbnailUrl)
                        if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                            RealDownloader.saveThumbnailToMediaStore(context, songId, thumbBytes)
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Failed to download thumbnail for $songId")
                    }
                }

                val fileLen = tempFile?.length() ?: 0L
                tempFile?.delete()

                database.query {
                    upsert(
                        FormatEntity(
                            id = songId,
                            itag = dbItag ?: 0,
                            mimeType = dbMimeType ?: "audio/webm",
                            codecs = dbCodecs ?: "opus",
                            bitrate = dbBitrate ?: 0,
                            sampleRate = dbSampleRate ?: 0,
                            contentLength = dbContentLength ?: tempFile?.length() ?: 0L,
                            loudnessDb = dbLoudnessDb,
                            playbackUrl = resolvedStreamUrl!!,
                        ),
                    )

                    val existingSong = getSongById(songId)?.song
                    if (existingSong != null) {
                        update(
                            existingSong.copy(
                                duration = if (existingSong.duration <= 0) dbDuration else existingSong.duration,
                                thumbnailUrl = dbThumbnailUrl ?: existingSong.thumbnailUrl,
                                dateDownload = LocalDateTime.now(),
                            )
                        )
                    } else {
                        upsert(
                            SongEntity(
                                id = songId,
                                title = title,
                                duration = dbDuration,
                                thumbnailUrl = dbThumbnailUrl,
                                dateDownload = LocalDateTime.now(),
                            )
                        )
                    }

                    if (dbArtist != null) {
                        val artistMaps = songArtistMap(songId)
                        if (artistMaps.isEmpty()) {
                            val artistId = ArtistEntity.generateArtistId()
                            upsert(ArtistEntity(id = artistId, name = dbArtist!!))
                            insert(SongArtistMap(songId = songId, artistId = artistId, position = 0))
                            Timber.tag(TAG).d("Saved artist '$dbArtist' + SongArtistMap for $songId")
                        } else {
                            Timber.tag(TAG).d("Artist maps already exist for $songId: ${artistMaps.map { it.artistId }}")
                        }
                    }
                    val savedSong = getSongById(songId)
                    Timber.tag(TAG).d("After DB save — song: ${savedSong?.song}, artists: ${savedSong?.artists?.joinToString { it.name }}")
                }

                updateDownloadState(
                    songId, request, Download.STATE_COMPLETED,
                    percentDownloaded = 100,
                    bytesDownloaded = fileLen,
                    contentLength = fileLen,
                )

                Timber.tag(TAG).i("✅ Download completed: $songId - $title ($fileLen bytes)")
                activeJobs.remove(songId)
                return

            } catch (e: Exception) {
                lastError = e
                Timber.tag(TAG).w(e, "⚠️ Download attempt ${attempt + 1}/${MAX_RETRIES + 1} failed: $songId - ${e.message}")

                tempFile?.delete()
                tempFile = null
                resolvedStreamUrl = null

                if (e !is IOException) break
            }
        }

        // All retries exhausted — final failure
        val finalError = lastError ?: IOException("Unknown download error")
        Timber.tag(TAG).e(finalError, "❌ Download failed after $MAX_RETRIES retries: $songId - $title")

        tempFile?.delete()

        updateDownloadState(
            songId, request, Download.STATE_FAILED,
            bytesDownloaded = 0,
            contentLength = 0,
            failureReason = Download.FAILURE_REASON_UNKNOWN,
        )

        activeJobs.remove(songId)
    }

    // ── State management ────────────────────────────────────────────────

    private fun updateDownloadState(
        songId: String,
        request: DownloadRequest,
        state: Int,
        percentDownloaded: Int = 0,
        bytesDownloaded: Long = 0L,
        contentLength: Long = -1L,
        failureReason: Int = Download.FAILURE_REASON_NONE,
    ) {
        val now = System.currentTimeMillis()
        val existing = _downloads.value[songId]

        _downloads.update { current: Map<String, Download> ->
            current + (songId to Download(
                request,
                state,
                existing?.startTimeMs ?: now,
                now,
                contentLength,
                0,  // stopReason
                failureReason,
                DownloadProgress().apply {
                    this.bytesDownloaded = bytesDownloaded
                    this.percentDownloaded = percentDownloaded.toFloat()
                },
            ))
        }
    }

    private fun updateDateDownloadInDb(songId: String, date: LocalDateTime?) {
        try {
            database.query {
                val existing = getSongById(songId)?.song ?: return@query
                val updatedSong = existing.copy(dateDownload = date)
                update(updatedSong)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update dateDownload for $songId")
        }
    }

    // ── Initialization: restore state from existing files ───────────────

    /**
     * Re-insert SongEntity records into Room DB for songs found in MediaStore
     * but missing from the database (e.g. after reinstall).
     */
    private suspend fun recoverSongEntities(
        collection: Uri,
        idsToRecover: List<String>,
    ) {
        val projection = arrayOf(
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val idsParam = idsToRecover.joinToString(",") { "?" }
        val selection = "${MediaStore.Audio.Media.COMPOSER} IN ($idsParam)"
        val now = LocalDateTime.now()
        context.contentResolver.query(collection, projection, selection, idsToRecover.toTypedArray(), null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val songId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)) ?: continue
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: songId
                val durationS = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000
                try {
                    database.query {
                        val existing = getSongById(songId)?.song
                        if (existing == null) {
                            upsert(
                                SongEntity(
                                    id = songId,
                                    title = title,
                                    duration = durationS.toInt(),
                                    dateDownload = now,
                                )
                            )
                        } else if (existing.dateDownload == null) {
                            update(existing.copy(dateDownload = now))
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to recover SongEntity for $songId")
                }
            }
        }
    }

    fun restoreDownloadStates() {
        val mediaStoreIds = RealDownloader.listMediaStoreIds(context)
        val oldIds = RealDownloader.listDownloadedIds(context)
        val allIds = (mediaStoreIds + oldIds).distinct()
        if (allIds.isEmpty()) {
            Timber.tag(TAG).d("No existing downloaded songs found")
            return
        }

        Timber.tag(TAG).d("Found ${allIds.size} existing downloaded songs")
        val states = allIds.mapNotNull { id ->
            val uri = RealDownloader.getSongUri(context, id)
            val file = RealDownloader.getSongFile(context, id)
            val lastModified = System.currentTimeMillis()
            val fileSize = uri?.let {
                try {
                    context.contentResolver.openAssetFileDescriptor(it, "r")?.use { afd ->
                        afd.length
                    } ?: 0L
                } catch (_: Exception) { 0L }
            } ?: file?.length() ?: return@mapNotNull null

            if (fileSize <= 0L) return@mapNotNull null

            val request = DownloadRequest.Builder(id, id.toUri()).build()
            id to Download(
                request,
                Download.STATE_COMPLETED,
                lastModified,
                lastModified,
                fileSize,
                0,
                0,
                DownloadProgress().apply {
                    this.bytesDownloaded = fileSize
                    this.percentDownloaded = 100f
                },
            )
        }.toMap<String, Download>()
        _downloads.value = states

        // Recover Room DB records for songs found in MediaStore but missing DB state
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        scope.launch {
            recoverSongEntities(collection, states.keys.toList())
        }
    }

    init {
        restoreDownloadStates()
        // Retry after delay to catch MediaStore scan completing (fresh installs)
        scope.launch {
            delay(5000)
            if (_downloads.value.isEmpty()) {
                Timber.tag(TAG).d("Retrying MediaStore restore after delay...")
                restoreDownloadStates()
            }
        }
    }

    /**
     * Cleanup when DownloadUtil is no longer needed.
     */
    fun destroy() {
        scope.cancel()
    }
}
