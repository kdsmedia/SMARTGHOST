package com.altomedia.smartghost.feature.player

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import com.altomedia.smartghost.coreui.component.SecondaryActionButton

/**
 * Single official YouTube player (Bab 6). Uses the YouTube IFrame Player API so
 * playback follows YouTube's rules and is driven only by the user's play/pause
 * actions. SMARTGHOST never creates multiple players or manipulates view counts.
 *
 * The host page is served from [PLAYER_ORIGIN] via [WebViewAssetLoader] rather
 * than `loadDataWithBaseURL`. A `data:` document has no real origin, so the
 * WebView sends no `Referer`/`Origin` to YouTube and the IFrame player refuses
 * to start with error 153 ("Video player configuration error"). Serving from a
 * real `https://` origin fixes that.
 *
 * A minimal JavaScript bridge relays the player's own error and state-change
 * callbacks back to the ViewModel so the UI can show honest status. The bridge
 * exposes no data and cannot read files or reflection — it only reports codes.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun PlayerScreen(
    videoId: String,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val safeVideoId = sanitizeVideoId(videoId)

    LaunchedEffect(safeVideoId) { viewModel.load(safeVideoId) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    val assetLoader = playerAssetLoader(context)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest,
                        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
                    }
                    addJavascriptInterface(PlayerBridge(viewModel), BRIDGE_NAME)
                    tag = safeVideoId
                    loadPlayer(safeVideoId)
                }
            },
            update = { webView: WebView ->
                if (webView.tag != safeVideoId) {
                    webView.tag = safeVideoId
                    webView.loadPlayer(safeVideoId)
                }
            },
        )

        Text(
            text = "Pemutaran dikendalikan sepenuhnya oleh Anda melalui pemutar " +
                "resmi YouTube.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SecondaryActionButton(
            text = "Buka di aplikasi/situs YouTube",
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = { onOpenInYouTube("https://www.youtube.com/watch?v=$safeVideoId") },
            modifier = Modifier.fillMaxWidth(),
        )

        state.lastError?.let { error ->
            Text(
                text = "Pemutar: $error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private const val BRIDGE_NAME = "SmartGhostBridge"

/**
 * Real HTTPS origin for the bundled player page. `appassets.androidplatform.net`
 * is the host [WebViewAssetLoader] reserves for local assets; using it gives the
 * page a proper origin (and therefore a Referer) without shipping a web server.
 */
internal const val PLAYER_ORIGIN = "https://appassets.androidplatform.net"

/** Asset path served to the player WebView. */
internal const val PLAYER_ASSET_PATH = "/assets/"

/** URL the player WebView loads, with the video id passed as a query parameter. */
internal fun playerUrl(videoId: String): String =
    "$PLAYER_ORIGIN/assets/player.html?videoId=" + Uri.encode(videoId)

/** Builds the asset loader that serves the bundled player page from an https origin. */
internal fun playerAssetLoader(context: android.content.Context): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .addPathHandler(PLAYER_ASSET_PATH, WebViewAssetLoader.AssetsPathHandler(context))
        .build()

/** Relays the official player's callbacks to the ViewModel. Reports codes only. */
private class PlayerBridge(private val viewModel: PlayerViewModel) {
    @JavascriptInterface
    fun onPlayerError(code: Int) {
        viewModel.onError(playerErrorMessage(code))
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        viewModel.onPlaybackStateChanged(state == 1)
    }
}

private fun WebView.loadPlayer(videoId: String) {
    loadUrl(playerUrl(videoId))
}

/**
 * Keeps only the characters YouTube video ids may contain, so the id cannot
 * break out of the script string (defence in depth; the id is already validated
 * upstream).
 */
internal fun sanitizeVideoId(videoId: String): String =
    videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(11)

/** Maps the IFrame API error codes to short, user-facing messages. */
internal fun playerErrorMessage(code: Int): String = when (code) {
    2 -> "Video ID tidak valid."
    5 -> "Tidak dapat diputar di pemutar HTML5."
    100 -> "Video tidak ditemukan atau bersifat privat."
    101, 150 -> "Pemilik video melarang pemutaran di luar YouTube."
    153 -> "Konfigurasi pemutar ditolak YouTube (Referer tidak valid)."
    else -> "Kesalahan pemutar (kode $code)."
}
