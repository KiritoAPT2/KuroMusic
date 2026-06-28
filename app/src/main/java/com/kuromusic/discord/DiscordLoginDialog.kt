package com.kuromusic.discord

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kuromusic.R
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginDialog(
    onToken: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var showManualInput by remember { mutableStateOf(false) }
    var manualToken by remember { mutableStateOf("") }
    var tokenExtracted by remember { mutableStateOf(false) }

    if (showManualInput) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Enter Discord Token",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "To get your token: open Discord web, press Ctrl+Shift+I, go to Application → Local Storage → copy the 'token' value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    OutlinedTextField(
                        value = manualToken,
                        onValueChange = { manualToken = it },
                        label = { Text("Token") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp),
                    )
                    TextButton(
                        onClick = {
                            if (manualToken.isNotBlank()) {
                                onToken(manualToken.trim())
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Discord Login") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Close",
                            )
                        }
                    },
                    actions = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                )

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true

                            // JavaScript bridge for token extraction
                            class TokenBridge {
                                @JavascriptInterface
                                fun onToken(token: String) {
                                    if (!tokenExtracted && token.length > 30) {
                                        tokenExtracted = true
                                        Timber.tag("DiscordSvc").d("Token extracted via JS bridge")
                                        Handler(Looper.getMainLooper()).post {
                                            onToken(token)
                                        }
                                    }
                                }
                            }
                            addJavascriptInterface(TokenBridge(), "KuroMusicBridge")

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    isLoading = false
                                    if (!tokenExtracted) {
                                        // Attempt 1: extract via JS from localStorage/sessionStorage
                                        injectTokenExtractionJs(view)
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): android.webkit.WebResourceResponse? {
                                    val url = request.url.toString()
                                    // Attempt 2: intercept Authorization header from API calls
                                    if (!tokenExtracted && url.startsWith("https://discord.com/api/v")) {
                                        val authHeader = request.requestHeaders["Authorization"]
                                        if (!authHeader.isNullOrEmpty() && authHeader.length > 50) {
                                            tokenExtracted = true
                                            Timber.tag("DiscordSvc").d("Token extracted via API header")
                                            onToken(authHeader)
                                        }
                                    }
                                    return null
                                }

                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    url: String,
                                ): Boolean {
                                    return false
                                }
                            }

                            loadUrl("https://discord.com/login")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                TextButton(
                    onClick = { showManualInput = true },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        "Or paste token manually",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun injectTokenExtractionJs(webView: WebView) {
    webView.evaluateJavascript(
        """(function(){
            try {
                // Try localStorage
                var raw = localStorage.getItem('token');
                if (raw) {
                    try { raw = JSON.parse(raw); } catch(e) {}
                    if (raw && raw.length > 30) {
                        window.KuroMusicBridge && window.KuroMusicBridge.onToken(raw);
                        return;
                    }
                }
                // Try sessionStorage as fallback
                raw = sessionStorage.getItem('token');
                if (raw) {
                    try { raw = JSON.parse(raw); } catch(e) {}
                    if (raw && raw.length > 30) {
                        window.KuroMusicBridge && window.KuroMusicBridge.onToken(raw);
                        return;
                    }
                }
            } catch(e) { console.log('KuroMusic token error: ' + e.message); }
        })()""",
        null,
    )
}
