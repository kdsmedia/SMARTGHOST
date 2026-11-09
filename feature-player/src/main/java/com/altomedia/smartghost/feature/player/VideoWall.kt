package com.altomedia.smartghost.feature.player

import android.graphics.BitmapFactory
import android.webkit.URLUtil
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.altomedia.smartghost.coreui.component.StatusDot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** One cell of the monitoring wall. */
data class WallCard(val index: Int, val videoId: String)

const val WALL_COLUMNS = 5
const val WALL_ROWS = 2
const val WALL_CARD_COUNT = WALL_COLUMNS * WALL_ROWS

/**
 * Builds the ten wall cells for a video. Pure so it can be unit-tested: the wall
 * always has [WALL_CARD_COUNT] cells and they all point at the same single video
 * the official player is showing.
 */
fun wallCards(videoId: String): List<WallCard> =
    (1..WALL_CARD_COUNT).map { WallCard(index = it, videoId = videoId) }

/**
 * A grid of ten mini cards (5 across, 2 down). Each card is a *preview* of the
 * one video the official player is showing: its public YouTube thumbnail plus a
 * live playback status. This is a monitoring wall — it is deliberately NOT ten
 * players, and it never claims to be a viewer count.
 */
@Composable
fun VideoWall(
    videoId: String,
    status: PlayerStatus,
    modifier: Modifier = Modifier,
) {
    val cards = wallCards(videoId)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "10 kartu pantau (5 × 2) — pratinjau video, bukan penonton",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(WALL_ROWS) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(WALL_COLUMNS) { colIndex ->
                    val card = cards[rowIndex * WALL_COLUMNS + colIndex]
                    WallCardView(
                        card = card,
                        status = status,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WallCardView(
    card: WallCard,
    status: PlayerStatus,
    modifier: Modifier = Modifier,
) {
    val thumbnail = rememberThumbnail(card.videoId)
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = "#${card.index}",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
        ) {
            StatusDot(level = status.toStatusLevel())
        }
    }
}

/**
 * Loads the public YouTube thumbnail for a video once per id. Failure (offline,
 * bad id) simply leaves the placeholder; it never blocks the UI.
 */
@Composable
private fun rememberThumbnail(videoId: String): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, videoId) {
        val url = thumbnailUrl(videoId)
        if (!URLUtil.isHttpsUrl(url)) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                connection.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    return state
}

/** Public YouTube thumbnail for a video id (medium quality, 320×180). */
internal fun thumbnailUrl(videoId: String): String =
    "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
