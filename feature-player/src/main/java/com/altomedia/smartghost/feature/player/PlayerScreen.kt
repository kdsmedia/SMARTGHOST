package com.altomedia.smartghost.feature.player

import android.annotation.SuppressLint
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
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    videoId: String,
    onOpenInYouTube: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(videoId) { viewModel.load(videoId) }

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
                    settings.mediaPlaybackRequiresUserGesture = true
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(
                        YOUTUBE_REFERRER,
                        buildPlayerHtml(videoId),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
            },
            update = { webView: WebView ->
                if (webView.tag != videoId) {
                    webView.tag = videoId
                    webView.loadDataWithBaseURL(
                        YOUTUBE_REFERRER,
                        buildPlayerHtml(videoId),
                        "text/html",
                        "utf-8",
                        null,
                    )
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
            onClick = { onOpenInYouTube("https://www.youtube.com/watch?v=$videoId") },
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

private const val YOUTUBE_REFERRER = "https://www.youtube.com"

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
                'onError': function(e) { onPlayerError(e.data); }
              }
            });
          }
          function onPlayerError(code) {
            // Report to the host app without leaking anything sensitive.
            console.log('SMARTGHOST_PLAYER_ERROR:' + code);
          }
        </script>
      </body>
    </html>
""".trimIndent()
