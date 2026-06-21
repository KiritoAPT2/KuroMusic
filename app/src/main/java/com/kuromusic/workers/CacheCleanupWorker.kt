package com.kuromusic.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.kuromusic.db.InternalDatabase
import com.kuromusic.utils.YTPlayerUtils
import java.io.File

class CacheCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun doWork(): Result {
        return try {
            val cacheDir = applicationContext.cacheDir
            val cutoff = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)

            Log.i("CacheCleanup", "Starting cache cleanup. Cutoff: $cutoff")

            var deletedCount = 0
            var reclaimedBytes = 0L

            cacheDir.walkTopDown().forEach { file ->
                val parentName = file.parentFile?.name
                if (parentName == "media" || parentName == "exoplayer") return@forEach
                if (file.isFile && file.lastModified() < cutoff) {
                    val size = file.length()
                    if (file.delete()) {
                        deletedCount++
                        reclaimedBytes += size
                    }
                }
            }

            // Clear Coil disk cache via API
            try {
                val coilCache = applicationContext.imageLoader.diskCache
                coilCache?.clear()
                Log.i("CacheCleanup", "Coil disk cache cleared via API")
            } catch (e: Exception) {
                Log.w("CacheCleanup", "Could not clear Coil cache via API", e)
            }

            // Prune old song history from Room DB (keep last 30 days)
            try {
                val db = InternalDatabase.newInstance(applicationContext)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                db.deleteSongHistoryOlderThan(thirtyDaysAgo)
                Log.i("CacheCleanup", "Pruned song history older than 30 days")
            } catch (e: Exception) {
                Log.w("CacheCleanup", "Could not prune song history", e)
            }

            // Trim stale entries from YTPlayerUtils in-memory cache
            YTPlayerUtils.trimCache()

            Log.i("CacheCleanup", "Cleanup complete. Deleted $deletedCount files, reclaimed ${reclaimedBytes / 1024} KB")
            Result.success()
        } catch (e: Exception) {
            Log.e("CacheCleanup", "Error during cache cleanup", e)
            Result.failure()
        }
    }
}
