package com.kuromusic.playback

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

/**
 * Legacy service stub.
 *
 * Previously extended Media3's [DownloadService] for cache-based downloads.
 * Now downloads are handled by [DownloadUtil] + [RealDownloader] which save
 * real .opus/.m4a files to filesDir/songs/.
 *
 * This stub exists for manifest compatibility. It is no longer actively started.
 */
class ExoDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.w("ExoDownloadService: received stale intent (new download system is active)")
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1
    }
}
