package com.kuromusic

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.YouTubeLocale
import com.kuromusic.kugou.KuGou
import com.kuromusic.constants.AccountChannelHandleKey
import com.kuromusic.constants.AccountEmailKey
import com.kuromusic.constants.AccountNameKey
import com.kuromusic.constants.ContentCountryKey
import com.kuromusic.constants.ContentLanguageKey
import com.kuromusic.constants.CountryCodeToName
import com.kuromusic.constants.DataSyncIdKey
import com.kuromusic.constants.InnerTubeCookieKey
import com.kuromusic.constants.LanguageCodeToName
import com.kuromusic.constants.MaxImageCacheSizeKey
import com.kuromusic.constants.ProxyEnabledKey
import com.kuromusic.constants.ProxyTypeKey
import com.kuromusic.constants.ProxyUrlKey
import com.kuromusic.constants.SYSTEM_DEFAULT
import com.kuromusic.constants.UseLoginForBrowse
import com.kuromusic.constants.VisitorDataKey
import com.kuromusic.extensions.toEnum
import com.kuromusic.extensions.toInetSocketAddress
import com.kuromusic.utils.dataStore
import com.kuromusic.utils.get
import com.kuromusic.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this;
        Timber.plant(Timber.DebugTree())

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")
        YouTube.apiKey = BuildConfig.INNER_TUBE_API_KEY

        // Batch-read all needed DataStore preferences in one go
        val prefs = runBlocking(Dispatchers.IO) { dataStore.data.first() }

        YouTube.locale = YouTubeLocale(
            gl = prefs[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = prefs[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (prefs[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    prefs[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    prefs[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (prefs[UseLoginForBrowse] != false) {
            YouTube.useLoginForBrowse = true
        }

        val scope = applicationScope
        scope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { storedData ->
                    val cached = storedData?.takeIf { it != "null" }
                    YouTube.visitorData = cached
                    // Always refresh visitor data on startup — cached data expires over time
                    YouTube.visitorData().onSuccess { newData ->
                        YouTube.visitorData = newData
                        if (newData != cached) {
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newData
                            }
                        }
                    }.onFailure {
                        if (cached == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT)
                                    .show()
                            }
                            reportException(it)
                        }
                    }
                }
        }
        scope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        /*
                         * Workaround to avoid breaking older installations that have a dataSyncId
                         * that contains "||" in it.
                         * If the dataSyncId ends with "||" and contains only one id, then keep the
                         * id before the "||".
                         * If the dataSyncId contains "||" and is not at the end, then keep the
                         * second id.
                         * This is needed to keep using the same account as before.
                         */
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        scope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        // we now allow user input now, here be the demons. This serves as a last ditch effort to avoid a crash loop
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        // will crash app if you set to 0 after cache starts being used
        if (cacheSize == 0) {
            return ImageLoader.Builder(this)
                .crossfade(true)
                .respectCacheHeaders(false)
                .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        val imageOkHttpClient = okhttp3.OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(20, 30, java.util.concurrent.TimeUnit.SECONDS))
            .build()

        return ImageLoader.Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .okHttpClient(imageOkHttpClient)
            // Memory cache for instant image display (128MB)
            .memoryCache(
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% of app memory
                    .strongReferencesEnabled(true)
                    .build()
            )
            // Disk cache
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            )
            // Placeholder downsampling
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .build()
    }

    companion object {
        lateinit var instance: App
            private set

        suspend fun forgetAccount(context: Context) {
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
            }
        }
    }
}