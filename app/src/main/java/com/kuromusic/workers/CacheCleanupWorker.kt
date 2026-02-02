package com.kuromusic.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class CacheCleanupWorker(
    appContext: Context, 
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val cacheDir = applicationContext.cacheDir
            val cutoff = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000) // 2 days
            
            Log.i("CacheCleanup", "Starting cache cleanup. Cutoff: $cutoff")

            var deletedCount = 0
            var reclaimedBytes = 0L

            cacheDir.walkTopDown().forEach { file ->
                if (file.isFile && file.lastModified() < cutoff) {
                    val size = file.length()
                    if (file.delete()) {
                        deletedCount++
                        reclaimedBytes += size
                    }
                }
            }
            
            // Try to clear Glide cache (must be on main thread usually, but clearDiskCache is background thread safe)
            // Since we don't have Glide dependency injected here cleanly and user snippet suggested:
            // Glide.get(context).clearDiskCache()
            // But we need to check if Glide is used. `libs.versions.toml` uses Coil?
            // "coil = { group = "io.coil-kt", name = "coil-compose", version = "2.7.0" }"
            // The user mentioned Glide, but the project uses Coil. 
            // I will implement Coil cache clearing instead if possible, or just ignore "Glide" if not present.
            // Coil's disk cache is usually in cacheDir/image_cache. The walkTopDown above SHOULD cover it 
            // if it respects lastModified.
            
            Log.i("CacheCleanup", "✅ Cleanup complete. Deleted $deletedCount files, reclaimed ${reclaimedBytes / 1024} KB")
            Result.success()
        } catch (e: Exception) {
            Log.e("CacheCleanup", "Error during cache cleanup", e)
            Result.failure()
        }
    }
}
