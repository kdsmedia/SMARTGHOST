package com.altomedia.smartghost.feature.player

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
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
import com.altomedia.smartghost.coreui.component.SecondaryActionButton

/**
 * Single official YouTube player (Bab 6). Uses the YouTube IFrame Player API so
 * playback follows YouTube's rules and is driven only by the user's play/pause
 * actions. SMARTGHOST never creates multiple players or manipulates view counts.
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
                    webViewClient = WebViewClient()
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
private const val YOUTUBE_REFERRER = "https://www.youtube.com"

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
    loadDataWithBaseURL(
        YOUTUBE_REFERRER,
        buildPlayerHtml(videoId),
        "text/html",
        "utf-8",
        null,
    )
}

/**
 * Keeps only the characters YouTube video ids may contain, so the id cannot
 * break out of the script string (defence in depth; the id is already validated
 * upstream).
 */
private fun sanitizeVideoId(videoId: String): String =
    videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(11)

/** Maps the IFrame API error codes to short, user-facing messages. */
private fun playerErrorMessage(code: Int): String = when (code) {
    2 -> "Video ID tidak valid."
    5 -> "Tidak dapat diputar di pemutar HTML5."
    100 -> "Video tidak ditemukan atau bersifat privat."
    101, 150 -> "Pemilik video melarang pemutaran di luar YouTube."
    else -> "Kesalahan pemutar (kode $code)."
}

/**
 * Minimal HTML host for the official IFrame Player API. A single player is
 * created; there is deliberately no loop, no autoplay chain and no automation.
 */
private fun buildPlayerHtml(videoId: String): String = """
    <!DOCTYPE html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          html, body { margin: 0; padding: 0; background: #000; height: 100%; }
          #player { width: 100%; height: 100%; }
        </style>
      </head>
      <body>
        <div id="player"></div>
        <script>
          var tag = document.createElement('script');
          tag.src = "https://www.youtube.com/iframe_api";
          document.head.appendChild(tag);

          var player;
          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              videoId: '$videoId',
              playerVars: { 'playsinline': 1, 'rel': 0 },
              events: {
                'onReady': function(e) { notifyState(e.target.getPlayerState()); },
                'onStateChange': function(e) { notifyState(e.data); },
                'onError': function(e) { notifyError(e.data); }
              }
            });
          }
          function notifyState(state) {
            if (window.SmartGhostBridge) { SmartGhostBridge.onStateChange(state); }
          }
          function notifyError(code) {
            if (window.SmartGhostBridge) { SmartGhostBridge.onPlayerError(code); }
          }
        </script>
      </body>
    </html>
""".trimIndent()
