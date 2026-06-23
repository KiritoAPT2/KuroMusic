package com.kuromusic.playback

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads full stream URLs to real files in [songDir].
 * Each song is saved as [songId].opus (or .m4a depending on mime type).
 *
 * Files are stored at: filesDir/songs/{songId}.opus
 * This makes them real, portable files — not Media3 cache fragments.
 */
object RealDownloader {
    private const val TAG = "RealDownloader"
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 60L
    /** Read buffer size: 256 KB for fast downloads */
    private const val BUFFER_SIZE = 256 * 1024L
    /** Log progress every ~5% */
    private const val PROGRESS_LOG_INTERVAL = 5

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Directory where downloaded song files are stored.
     */
    fun getSongDir(context: Context): File {
        return File(context.filesDir, "songs").also { it.mkdirs() }
    }

    /**
     * Returns the .opus file for a given song ID, or null if not downloaded.
     */
    fun getSongFile(context: Context, songId: String): File? {
        val opusFile = File(getSongDir(context), "$songId.opus")
        if (opusFile.exists()) return opusFile
        val m4aFile = File(getSongDir(context), "$songId.m4a")
        if (m4aFile.exists()) return m4aFile
        return null
    }

    /**
     * Resolves the file extension based on mime type.
     */
    fun extensionForMime(mimeType: String): String {
        return when {
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
            mimeType.contains("webm") || mimeType.contains("opus") -> "opus"
            else -> "opus" // default
        }
    }

    /**
     * Download status callback.
     */
    fun interface ProgressCallback {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }

    /**
     * Downloads a complete stream URL to a file.
     *
     * @param url The stream URL to download
     * @param destination The destination file (will be overwritten if exists)
     * @param progress Optional progress callback
     * @return The downloaded file
     * @throws IOException if download fails
     */
    @Throws(IOException::class)
    fun downloadStream(
        url: String,
        destination: File,
        progress: ProgressCallback? = null,
    ): File {
        Timber.tag(TAG).d("Starting download: ${destination.name}")
        Timber.tag(TAG).d("URL: ${url.take(100)}...")

        destination.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Kuromusic/1.0")
            .build()

        val response: Response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.closeQuietly()
            throw IOException("Server returned ${response.code}")
        }

        val body = response.body ?: run {
            response.closeQuietly()
            throw IOException("Empty response body")
        }

        val contentLength = body.contentLength()
        Timber.tag(TAG).d("Content-Length: $contentLength")

        val source = body.source()
        val sink = destination.sink().buffer()

        try {
            var totalRead = 0L
            var lastLogPercent = 0
            val okBuffer = okio.Buffer()
            val reportInterval = maxOf(1L, contentLength / 100) // ~1% increments
            var nextReport = reportInterval

            while (true) {
                val bytesRead = source.read(okBuffer, BUFFER_SIZE)
                if (bytesRead == -1L) break
                totalRead += bytesRead
                sink.write(okBuffer, bytesRead)

                if (totalRead >= nextReport) {
                    progress?.onProgress(totalRead, contentLength)
                    nextReport += reportInterval

                    // Log progress every ~5%
                    val pct = if (contentLength > 0) (totalRead * 100 / contentLength).toInt() else -1
                    if (pct >= lastLogPercent + PROGRESS_LOG_INTERVAL) {
                        lastLogPercent = pct
                        Timber.tag(TAG).d("⏳ ${destination.name}: %d%% (%d/%d bytes)", pct, totalRead, contentLength)
                    }
                }
            }

            sink.flush()
            Timber.tag(TAG).i("✅ Download complete: ${destination.absolutePath} ($totalRead bytes)")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Download failed: ${destination.name}")
            // Delete partial file
            destination.delete()
            throw IOException("Download failed: ${e.message}", e)
        } finally {
            sink.closeQuietly()
            response.closeQuietly()
        }

        return destination
    }

    /**
     * Deletes a downloaded song file.
     */
    fun deleteSongFile(context: Context, songId: String): Boolean {
        val file = getSongFile(context, songId)
        return if (file != null) {
            val deleted = file.delete()
            Timber.tag(TAG).d("Deleted ${file.name}: $deleted")
            deleted
        } else {
            false
        }
    }

    /**
     * Lists all downloaded song IDs by scanning the songs directory.
     */
    fun listDownloadedIds(context: Context): List<String> {
        val dir = getSongDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && (it.extension == "opus" || it.extension == "m4a") }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    /**
     * Returns total size of all downloaded songs in bytes.
     */
    fun getTotalDownloadSize(context: Context): Long {
        val dir = getSongDir(context)
        if (!dir.exists()) return 0L
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() }
            ?: 0L
    }
}
