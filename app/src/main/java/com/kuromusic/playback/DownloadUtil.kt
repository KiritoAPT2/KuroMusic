package com.kuromusic.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import com.kuromusic.constants.AudioQuality
import com.kuromusic.constants.AudioQualityKey
import com.kuromusic.db.MusicDatabase
import com.kuromusic.db.entities.FormatEntity
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
        // Cancel any active download
        activeJobs[songId]?.cancel()
        activeJobs.remove(songId)

        // Delete the file
        RealDownloader.deleteSongFile(context, songId)

        // Remove from state
        _downloads.update { it - songId }

        // Update DB: clear dateDownload
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
        var destinationFile: File? = null
        var resolvedStreamUrl: String? = null
        // Store metadata fields for DB upsert when download succeeds
        var dbItag: Int? = null
        var dbMimeType: String? = null
        var dbCodecs: String? = null
        var dbBitrate: Int? = null
        var dbSampleRate: Int? = null
        var dbContentLength: Long? = null
        var dbLoudnessDb: Double? = null

        // Retry loop for transient network errors (exponential backoff)
        for (attempt in 0..MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)) // 1s, 2s, 4s
                Timber.tag(TAG).w("Retry $attempt/$MAX_RETRIES for $songId after ${delayMs}ms...")
                delay(delayMs)
            }

            try {
                // Update state: DOWNLOADING
                if (attempt == 0) {
                    updateDownloadState(songId, request, Download.STATE_DOWNLOADING)
                }

                // 1. Resolve stream URL (only once — cache it on success)
                if (resolvedStreamUrl == null) {
                    val playbackData = YTPlayerUtils.playerResponseForPlayback(
                        videoId = songId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    ).getOrThrow()

                    resolvedStreamUrl = playbackData.streamUrl
                    val fmt = playbackData.format
                    val ext = RealDownloader.extensionForMime(fmt.mimeType ?: "")
                    destinationFile = File(RealDownloader.getSongDir(context), "$songId.$ext")

                    // Cache metadata for DB
                    dbItag = fmt.itag
                    dbMimeType = (fmt.mimeType ?: "audio/webm").split(";")[0]
                    dbCodecs = try {
                        (fmt.mimeType ?: "").split("codecs=")[1].removeSurrounding("\"")
                    } catch (_: Exception) { "opus" }
                    dbBitrate = fmt.bitrate
                    dbSampleRate = fmt.audioSampleRate
                    dbContentLength = fmt.contentLength
                    dbLoudnessDb = playbackData.audioConfig?.loudnessDb

                    Timber.tag(TAG).d("Downloading to: ${destinationFile.absolutePath}")
                }

                // 2. Download the full stream to file
                RealDownloader.downloadStream(
                    url = resolvedStreamUrl!!,
                    destination = destinationFile!!,
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

                // 3. Save format metadata to DB
                database.query {
                    upsert(
                        FormatEntity(
                            id = songId,
                            itag = dbItag ?: 0,
                            mimeType = dbMimeType ?: "audio/webm",
                            codecs = dbCodecs ?: "opus",
                            bitrate = dbBitrate ?: 0,
                            sampleRate = dbSampleRate ?: 0,
                            contentLength = dbContentLength ?: destinationFile!!.length(),
                            loudnessDb = dbLoudnessDb,
                            playbackUrl = resolvedStreamUrl!!,
                        ),
                    )
                }

                // 4. Update DB: mark song as downloaded
                updateDateDownloadInDb(songId, LocalDateTime.now())

                // 5. Mark as completed
                val fileLen = destinationFile!!.length()
                updateDownloadState(
                    songId, request, Download.STATE_COMPLETED,
                    percentDownloaded = 100,
                    bytesDownloaded = fileLen,
                    contentLength = fileLen,
                )

                Timber.tag(TAG).i("✅ Download completed: $songId - $title ($fileLen bytes)")
                activeJobs.remove(songId)
                return // Success — exit retry loop

            } catch (e: Exception) {
                lastError = e
                Timber.tag(TAG).w(e, "⚠️ Download attempt ${attempt + 1}/${MAX_RETRIES + 1} failed: $songId - ${e.message}")

                // Delete partial file — will re-download on retry
                destinationFile?.delete()
                destinationFile = null
                resolvedStreamUrl = null // Re-resolve URL on retry

                // Only retry on IOExceptions (transient network errors)
                if (e !is IOException) break
            }
        }

        // All retries exhausted — final failure
        val finalError = lastError ?: IOException("Unknown download error")
        Timber.tag(TAG).e(finalError, "❌ Download failed after $MAX_RETRIES retries: $songId - $title")

        destinationFile?.delete()

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

    private fun restoreDownloadStates() {
        val downloadedIds = RealDownloader.listDownloadedIds(context)
        if (downloadedIds.isEmpty()) {
            Timber.tag(TAG).d("No existing downloaded songs found")
            return
        }

        Timber.tag(TAG).d("Found ${downloadedIds.size} existing downloaded songs")
        val states = downloadedIds.mapNotNull { id ->
            val file = RealDownloader.getSongFile(context, id) ?: return@mapNotNull null
            val request = DownloadRequest.Builder(id, id.toUri()).build()
            id to Download(
                request,
                Download.STATE_COMPLETED,
                file.lastModified(),
                file.lastModified(),
                file.length(),
                0,  // stopReason
                0,  // failureReason
                DownloadProgress().apply {
                    this.bytesDownloaded = file.length()
                    this.percentDownloaded = 100f
                },
            )
        }.toMap<String, Download>()
        _downloads.value = states
    }

    init {
        restoreDownloadStates()
    }

    /**
     * Cleanup when DownloadUtil is no longer needed.
     */
    fun destroy() {
        scope.cancel()
    }
}
