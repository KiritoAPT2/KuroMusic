package com.kuromusic

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.Coil
import coil.request.ImageRequest
import com.kuromusic.playback.MusicService
import com.kuromusic.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MusicWidget : AppWidgetProvider() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isUpdating = false

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        startProgressUpdater(context)
    }

    override fun onEnabled(context: Context) {
        startProgressUpdater(context)
    }

    override fun onDisabled(context: Context) {
        stopProgressUpdater()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_OPEN_APP -> {
                openApp(context)
            }

            ACTION_STATE_CHANGED, ACTION_UPDATE_PROGRESS -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun openApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdater(context: Context) {
        if (isUpdating) return

        isUpdating = true
        runnable = Runnable {
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player

            // Solo actualizar si hay música reproduciéndose o pausada
            if (player != null && (player.isPlaying || player.playbackState == Player.STATE_READY)) {
                updateAllWidgets(context)
                handler.postDelayed(runnable, 1000)
            } else {
                // Si no hay música, reducir la frecuencia de actualización
                updateAllWidgets(context)
                handler.postDelayed(runnable, 5000)
            }
        }
        handler.post(runnable)
    }

    private fun stopProgressUpdater() {
        isUpdating = false
        handler.removeCallbacks(runnable)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.kuromusic.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.kuromusic.ACTION_PREV"
        const val ACTION_NEXT = "com.kuromusic.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.kuromusic.ACTION_SHUFFLE"
        const val ACTION_LIKE = "com.kuromusic.ACTION_LIKE"
        const val ACTION_REPLAY = "com.kuromusic.ACTION_REPLAY"
        const val ACTION_OPEN_APP = "com.kuromusic.ACTION_OPEN_APP"
        const val ACTION_STATE_CHANGED = "com.kuromusic.ACTION_STATE_CHANGED"
        const val ACTION_UPDATE_PROGRESS = "com.kuromusic.ACTION_UPDATE_PROGRESS"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                widgetIds.forEach { updateWidget(context, appWidgetManager, it) }
            }
        }

        private var cachedAlbumArtUrl: String? = null
        private var cachedAlbumArtBitmap: Bitmap? = null

        private fun loadAlbumArtBitmap(context: Context, url: String): Bitmap? {
            if (url == cachedAlbumArtUrl && cachedAlbumArtBitmap != null) {
                return cachedAlbumArtBitmap
            }
            return try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(160, 160)
                    .build()
                val drawable = runBlocking(Dispatchers.IO) {
                    Coil.imageLoader(context).execute(request).drawable
                }
                drawable?.toBitmap()?.also {
                    cachedAlbumArtUrl = url
                    cachedAlbumArtBitmap = it
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player

            setPendingIntents(context, views)

            player?.let { player ->
                val songTitle = player.mediaMetadata.title?.toString()
                    ?: context.getString(R.string.song_title)
                val artist = player.mediaMetadata.artist?.toString()
                    ?: context.getString(R.string.artist_name)

                views.setTextViewText(R.id.widget_song_title, songTitle)
                views.setTextViewText(R.id.widget_artist, artist)

                val playPauseIcon = if (player.isPlaying) R.drawable.pause else R.drawable.play
                views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

                val shuffleIcon = if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                views.setImageViewResource(R.id.widget_shuffle, shuffleIcon)

                val likeIcon = if (playerConnection.isCurrentSongLiked())
                    R.drawable.favorite else R.drawable.favorite_border
                views.setImageViewResource(R.id.widget_like, likeIcon)

                val currentPos = player.currentPosition
                val duration = player.duration
                views.setTextViewText(R.id.widget_current_time, formatTime(currentPos))
                views.setTextViewText(R.id.widget_duration, formatTime(duration))

                val progress = if (duration > 0 && duration != Long.MAX_VALUE) {
                    (currentPos * 100 / duration).toInt()
                } else 0

                if (duration > 0 && duration != Long.MAX_VALUE) {
                    views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
                }

                val playbackStateText = when {
                    player.repeatMode == Player.REPEAT_MODE_ONE -> context.getString(R.string.repeat_mode_one)
                    player.repeatMode == Player.REPEAT_MODE_ALL -> context.getString(R.string.repeat_mode_all)
                    else -> ""
                }

                if (playbackStateText.isNotEmpty()) {
                    views.setTextViewText(R.id.widget_playback_state, playbackStateText)
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.GONE)
                }

                // Album art sincrónico con cache
                val thumbnailUrl = player.mediaMetadata.artworkUri?.toString()
                if (!thumbnailUrl.isNullOrEmpty()) {
                    val bitmap = loadAlbumArtBitmap(context, thumbnailUrl)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                }

                if (player.mediaItemCount == 0) {
                    views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                    views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
                }
            } ?: run {
                // App minimizada - mostrar última info con portada cacheada
                views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)

                cachedAlbumArtBitmap?.let {
                    views.setImageViewBitmap(R.id.widget_album_art, it)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            val playPausePendingIntent = getServicePendingIntent(context, ACTION_PLAY_PAUSE)
            val prevPendingIntent = getServicePendingIntent(context, ACTION_PREV)
            val nextPendingIntent = getServicePendingIntent(context, ACTION_NEXT)
            val shufflePendingIntent = getServicePendingIntent(context, ACTION_SHUFFLE)
            val likePendingIntent = getServicePendingIntent(context, ACTION_LIKE)
            val openAppPendingIntent = getBroadcastPendingIntent(context, ACTION_OPEN_APP)

            // Controles de reproducción
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shufflePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_like, likePendingIntent)

            // Área principal del widget para abrir la app
            views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openAppPendingIntent)

            // También hacer que el área del progress bar abra la app
            views.setOnClickPendingIntent(R.id.widget_progress_bar, openAppPendingIntent)
        }

        private fun getServicePendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicService::class.java).apply {
                this.action = action
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            return PendingIntent.getService(context, action.hashCode(), intent, flags)
        }

        private fun getBroadcastPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java).apply {
                this.action = action
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
        }

        @SuppressLint("DefaultLocale")
        private fun formatTime(millis: Long): String {
            return if (millis < 0 || millis == Long.MAX_VALUE) "0:00" else String.format(
                "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            )
        }
    }
}