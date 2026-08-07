package com.kuromusic.playback

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

object RealDownloader {
    private const val TAG = "RealDownloader"
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 60L
    private const val BUFFER_SIZE = 1024L * 1024L
    private const val PROGRESS_LOG_INTERVAL = 5
    private const val YT_MUSIC_USER_AGENT = "com.google.android.apps.youtube.music/7.01.52 (Linux; U; Android 15; Pixel 9 Pro)"
    private const val KUROMUSIC_DIR = "KuroMusic"
    private const val THUMBS_DIR = "thumbnails"
    private const val MAX_FILENAME_LENGTH = 180

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(MAX_FILENAME_LENGTH)
            .ifEmpty { "unknown" }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor(YouTubeSessionInterceptor())
            .build()
    }

    @Deprecated("Use saveToMediaStore instead. Kept for migrating old downloads.")
    fun getSongDir(context: Context): File {
        return File(context.filesDir, "songs").also { it.mkdirs() }
    }

    fun getSongFile(context: Context, songId: String): File? {
        val opusFile = File(getSongDir(context), "$songId.opus")
        if (opusFile.exists()) return opusFile
        val m4aFile = File(getSongDir(context), "$songId.m4a")
        if (m4aFile.exists()) return m4aFile
        return null
    }

    fun extensionForMime(mimeType: String): String {
        return when {
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
            mimeType.contains("webm") || mimeType.contains("opus") -> "opus"
            else -> "opus"
        }
    }

    fun interface ProgressCallback {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }

    @Throws(IOException::class)
    fun downloadImageBytes(url: String): ByteArray? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().body?.bytes()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to download image bytes from $url")
            null
        }
    }

    fun downloadStream(
        url: String,
        destination: File,
        progress: ProgressCallback? = null,
    ): File {
        Timber.tag(TAG).d("Starting download: ${destination.name}")
        Timber.tag(TAG).d("URL length: ${url.length}")

        destination.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", YT_MUSIC_USER_AGENT)
            .header("Range", "bytes=0-")
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
            val reportInterval = maxOf(1L, contentLength / 100)
            var nextReport = reportInterval

            while (true) {
                val bytesRead = source.read(okBuffer, BUFFER_SIZE)
                if (bytesRead == -1L) break
                totalRead += bytesRead
                sink.write(okBuffer, bytesRead)

                if (totalRead >= nextReport) {
                    progress?.onProgress(totalRead, contentLength)
                    nextReport += reportInterval

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
            destination.delete()
            throw IOException("Download failed: ${e.message}", e)
        } finally {
            sink.closeQuietly()
            response.closeQuietly()
        }

        return destination
    }

    @Throws(IOException::class)
    fun downloadStreamToTemp(
        url: String,
        songId: String,
        mimeType: String,
        context: Context,
        progress: ProgressCallback? = null,
    ): File {
        val ext = extensionForMime(mimeType)
        val tempFile = File(context.cacheDir, "${songId}_temp.$ext")
        tempFile.parentFile?.mkdirs()
        return downloadStream(url, tempFile, progress)
    }

    fun saveToMediaStore(context: Context, songId: String, title: String?, artist: String?, mimeType: String, sourceFile: File): Uri? {
        val ext = extensionForMime(mimeType)
        val readableName = buildString {
            append(sanitizeFileName(songId))
            if (!title.isNullOrBlank()) {
                append(" - ").append(sanitizeFileName(title))
                if (!artist.isNullOrBlank()) {
                    append(" - ").append(sanitizeFileName(artist))
                }
            }
        }.take(MAX_FILENAME_LENGTH).ifEmpty { songId }
        val displayName = "$readableName.$ext"
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$KUROMUSIC_DIR")
            put(MediaStore.Audio.Media.IS_MUSIC, true)
            put(MediaStore.Audio.Media.TITLE, title ?: songId)
            put(MediaStore.Audio.Media.ARTIST, artist ?: "Unknown")
            put(MediaStore.Audio.Media.COMPOSER, songId)
            put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues) ?: run {
            Timber.tag(TAG).e("Failed to create MediaStore entry for $songId")
            return null
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(outputStream, BUFFER_SIZE.toInt())
                }
            }
            val updateValues = ContentValues().apply {
                put(MediaStore.Audio.Media.COMPOSER, songId)
                put(MediaStore.Audio.Media.TITLE, title ?: songId)
                put(MediaStore.Audio.Media.ARTIST, artist ?: "Unknown")
            }
            context.contentResolver.update(uri, updateValues, null, null)
            Timber.tag(TAG).i("Saved to MediaStore: $uri")
            return uri
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to write to MediaStore for $songId")
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }

    fun getSongUri(context: Context, songId: String): Uri? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        // COMPOSER is always the songId — most reliable lookup
        val compSelection = "${MediaStore.Audio.Media.COMPOSER} = ?"
        val compArgs = arrayOf(songId)
        context.contentResolver.query(collection, projection, compSelection, compArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        // Fallback to DISPLAY_NAME starting with songId (old naming scheme)
        val dnSelection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?"
        val dnArgs = arrayOf("$songId%")
        context.contentResolver.query(collection, projection, dnSelection, dnArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return null
    }

    data class LocalAudioFile(
        val uri: Uri,
        val songId: String?,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val size: Long,
        val mimeType: String?,
        val thumbnailUri: Uri?,
    )

    fun listLocalAudioFiles(context: Context): List<LocalAudioFile> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$KUROMUSIC_DIR%", "%${KUROMUSIC_DIR.lowercase()}%")
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val files = mutableListOf<LocalAudioFile>()

        // Batch-query thumbnails first
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val thumbMap = mutableMapOf<String, Uri>()
        val thumbProjection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val thumbPaths = listOf(
            "%$KUROMUSIC_DIR/$THUMBS_DIR%",
            "%$KUROMUSIC_DIR/.thumbnails%"
        )
        for (thumbPath in thumbPaths) {
            context.contentResolver.query(
                imageCollection, thumbProjection,
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?", arrayOf(thumbPath), null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: continue
                    val sid = name.removeSuffix(".jpg")
                    if (sid !in thumbMap) {
                        thumbMap[sid] = Uri.withAppendedPath(imageCollection, id.toString())
                    }
                }
            }
        }

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val composer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))
                val uri = Uri.withAppendedPath(collection, id.toString())
                val songId = composer?.takeIf { !it.isNullOrBlank() }
                    ?: displayName?.substringBefore(" - ")?.substringBeforeLast(".")?.takeIf { it.isNotBlank() }
                files.add(
                    LocalAudioFile(
                        uri = uri,
                        songId = songId,
                        title = title ?: "Unknown",
                        artist = artist ?: "Unknown Artist",
                        durationMs = duration,
                        size = size,
                        mimeType = mime,
                        thumbnailUri = songId?.let { thumbMap[it] ?: findThumbnailOnDisk(context, it) },
                    )
                )
            }
        }
        Timber.tag(TAG).d("listLocalAudioFiles: found ${files.size} files")
        return files
    }

    fun saveThumbnailToMediaStore(context: Context, songId: String, imageBytes: ByteArray): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$songId.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$KUROMUSIC_DIR/$THUMBS_DIR")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = context.contentResolver.insert(collection, contentValues) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(imageBytes)
                outputStream.flush()
            }
            val pendingValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, pendingValues, null, null)
            Timber.tag(TAG).i("Saved thumbnail to MediaStore: $uri")
            uri
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save thumbnail for $songId")
            null
        }
    }

    fun getThumbnailUri(context: Context, songId: String): Uri? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val paths = listOf(
            "%$KUROMUSIC_DIR/$THUMBS_DIR%",
            "%$KUROMUSIC_DIR/.thumbnails%"
        )
        for (pathPattern in paths) {
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val args = arrayOf("$songId.jpg", pathPattern)
            context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    return Uri.withAppendedPath(collection, id.toString())
                }
            }
        }
        val fallbackSelection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val fallbackArgs = arrayOf("$songId.jpg")
        context.contentResolver.query(collection, projection, fallbackSelection, fallbackArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return Uri.withAppendedPath(collection, id.toString())
            }
        }
        return findThumbnailOnDisk(context, songId)
    }

    private fun findThumbnailOnDisk(context: Context, songId: String): Uri? {
        return try {
            val thumbsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "$KUROMUSIC_DIR/$THUMBS_DIR"
            )
            val thumbFile = File(thumbsDir, "$songId.jpg")
            if (!thumbFile.exists()) return null

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$songId.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$KUROMUSIC_DIR/$THUMBS_DIR")
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            context.contentResolver.insert(collection, contentValues)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteThumbnail(context: Context, songId: String): Boolean {
        val uri = getThumbnailUri(context, songId) ?: return false
        val deleted = context.contentResolver.delete(uri, null, null)
        return deleted > 0
    }

    fun deleteFromMediaStore(context: Context, songId: String): Boolean {
        val uri = getSongUri(context, songId) ?: return false
        val deleted = context.contentResolver.delete(uri, null, null)
        deleteThumbnail(context, songId)
        Timber.tag(TAG).d("Deleted MediaStore entry for $songId: $deleted")
        return deleted > 0
    }

    fun listMediaStoreIds(context: Context): List<String> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val ids = mutableSetOf<String>()

        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$KUROMUSIC_DIR%")
        val projection = arrayOf(MediaStore.Audio.Media.COMPOSER, MediaStore.Audio.Media.DISPLAY_NAME)
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val composer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER))
                if (!composer.isNullOrBlank()) {
                    ids.add(composer)
                } else {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    if (!name.isNullOrBlank()) {
                        val nameNoExt = name.removeSuffix(".opus").removeSuffix(".webm").removeSuffix(".m4a").removeSuffix(".mp3")
                        val id = nameNoExt.substringBefore(" - ").takeIf { it.isNotBlank() } ?: nameNoExt
                        if (id.isNotBlank()) ids.add(id)
                    }
                }
            }
        }

        Timber.tag(TAG).d("listMediaStoreIds: found ${ids.size} songs: $ids")
        return ids.toList()
    }

    fun getMediaStoreTotalSize(context: Context): Long {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$KUROMUSIC_DIR%")
        val projection = arrayOf(MediaStore.Audio.Media.SIZE)
        var total = 0L
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                total += cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
            }
        }
        return total
    }

    @Deprecated("Use deleteFromMediaStore instead")
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

    @Deprecated("Use listMediaStoreIds instead")
    fun listDownloadedIds(context: Context): List<String> {
        val dir = getSongDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && (it.extension == "opus" || it.extension == "m4a") }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    @Deprecated("Use getMediaStoreTotalSize instead")
    fun getTotalDownloadSize(context: Context): Long {
        val dir = getSongDir(context)
        if (!dir.exists()) return 0L
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() }
            ?: 0L
    }
}
