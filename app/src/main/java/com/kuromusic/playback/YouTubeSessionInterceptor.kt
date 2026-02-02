package com.kuromusic.playback

import com.kuromusic.innertube.YouTube
import com.kuromusic.innertube.models.YouTubeClient
import com.kuromusic.innertube.utils.sha1
import com.kuromusic.innertube.utils.parseCookieString
import com.kuromusic.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class YouTubeSessionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val cookie = YouTube.cookie ?: BuildConfig.YOUTUBE_SESSION_COOKIES

        // Si no hay sesión, simplemente procedemos
        if (cookie.isBlank()) {
            return chain.proceed(originalRequest)
        }

        // Caso con sesión activa
        val requestWithSession = originalRequest.newBuilder()
            .apply {
                header("Cookie", cookie)
                header("X-Goog-AuthUser", "0")
                header("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                header("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)

                // Calcular SAPISIDHASH si SAPISID existe
                val cookieMap = parseCookieString(cookie)
                if (cookieMap.containsKey("SAPISID")) {
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    header("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
            .build()

        val response = chain.proceed(requestWithSession)

        // Si falla con sesión (403), intentamos fallback como invitado
        if (response.code == 403) {
            Timber.tag("YouTubeSession").w("Session request failed (403). Retrying as guest fallback...")
            response.close() // Importante cerrar la respuesta fallida
            
            val guestRequest = originalRequest.newBuilder()
                .removeHeader("Cookie")
                .removeHeader("Authorization")
                .removeHeader("X-Goog-AuthUser")
                .build()
            
            return chain.proceed(guestRequest)
        }

        return response
    }
}
