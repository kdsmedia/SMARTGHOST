package com.altomedia.smartghost.feature.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import com.altomedia.smartghost.coreui.component.StatusDot
import com.altomedia.smartghost.coreui.component.StatusLevel

/**
 * Single official YouTube player (Bab 6). The page embeds YouTube's official
 * `youtube.com/embed/<id>` player as a plain iframe, so playback follows
 * YouTube's rules and is driven only by the user's play/pause actions.
 * SMARTGHOST never creates multiple players or manipulates view counts.
 *
 * The host page is served from [PLAYER_ORIGIN] via [WebViewAssetLoader] rather
 * than `loadDataWithBaseURL`. A `data:` document has no real origin, so the
 * embedded frame receives no `Referer` and YouTube may refuse playback. Serving
 * from a real `https://` origin fixes that.
 *
 * A minimal JavaScript bridge relays the player's own error and state-change
 * callbacks back to the ViewModel so the UI can show honest status. The bridge
 * exposes no data and cannot read files or reflection — it only reports codes.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun PlayerScreen(
    videoId: String,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
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

        PlaybackStatusRow(status = state.status, videoId = safeVideoId)

        Text(
            text = "Pemutaran dikendalikan sepenuhnya oleh Anda melalui pemutar " +
                "resmi YouTube.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Dot + label for the current playback state, plus the embedded video id. */
@Composable
private fun PlaybackStatusRow(status: PlayerStatus, videoId: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(level = status.toStatusLevel())
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "• video $videoId",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(2.dp))
}

internal fun PlayerStatus.toStatusLevel(): StatusLevel = when (this) {
    PlayerStatus.PLAYING -> StatusLevel.SUCCESS
    PlayerStatus.BUFFERING, PlayerStatus.LOADING -> StatusLevel.RUNNING
    PlayerStatus.ERROR -> StatusLevel.FAILURE
    else -> StatusLevel.NEUTRAL
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
internal fun playerAssetLoader(context: Context): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .addPathHandler(PLAYER_ASSET_PATH, WebViewAssetLoader.AssetsPathHandler(context))
        .build()

/** Relays the official player's callbacks to the ViewModel. Reports codes only. */
private class PlayerBridge(private val viewModel: PlayerViewModel) {
    @JavascriptInterface
    fun onPlayerError(code: Int) {
        viewModel.onError(playerErrorMessage(code))
    }

    /** Fallback text from the page itself (offline, blocked, or slow network). */
    @JavascriptInterface
    fun onPlayerErrorText(message: String) {
        viewModel.onError(message)
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        viewModel.onPlaybackStateChanged(state)
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
fun sanitizeVideoId(videoId: String): String =
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
