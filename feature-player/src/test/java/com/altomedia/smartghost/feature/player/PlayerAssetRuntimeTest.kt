package com.altomedia.smartghost.feature.player

import android.webkit.WebResourceResponse
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runtime proof that the player host page is served from a real https origin and
 * embeds the official YouTube player as a plain iframe.
 *
 * The host page must be served over a real https origin (not `data:`) so the
 * embedded YouTube frame receives a Referer and the video loads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlayerAssetRuntimeTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun playerHtml(videoId: String = "dQw4w9WgXcQ"): String {
        val response: WebResourceResponse? =
            playerAssetLoader(context).shouldInterceptRequest(
                android.net.Uri.parse(playerUrl(videoId)),
            )
        assertNotNull("player.html must be served from assets", response)
        return response!!.data.bufferedReader().use { it.readText() }
    }

    @Test
    fun `player url uses the https asset origin`() {
        val url = playerUrl("dQw4w9WgXcQ")
        assertTrue(url.startsWith("$PLAYER_ORIGIN/assets/player.html"))
        assertTrue(url.contains("videoId=dQw4w9WgXcQ"))
    }

    @Test
    fun `asset loader serves the bundled player page`() {
        val html = playerHtml()
        assertTrue(html.contains("get('videoId')"))
        assertTrue(html.contains("youtube.com/embed/"))
        assertTrue(html.contains("referrer"))
    }

    @Test
    fun `player page embeds the official iframe with playback support`() {
        val html = playerHtml()
        assertTrue("playsinline is set", html.contains("playsinline=1"))
        assertTrue("fullscreen is allowed", html.contains("allowfullscreen"))
        assertTrue("state events are relayed", html.contains("onStateChange"))
    }

    @Test
    fun `player page reports failures instead of an empty black box`() {
        val html = playerHtml()
        assertTrue("fallback UI is present", html.contains("showFallback"))
        assertTrue("missing id is handled", html.contains("ID video tidak ditemukan"))
        assertTrue("bridge can report fallback text", html.contains("onPlayerErrorText"))
    }

    @Test
    fun `error 153 has a specific user-facing message`() {
        assertEquals(
            "Konfigurasi pemutar ditolak YouTube (Referer tidak valid).",
            playerErrorMessage(153),
        )
    }

    @Test
    fun `video id sanitisation strips unsafe characters`() {
        assertEquals("dQw4w9WgXcQ", sanitizeVideoId("dQw4w9WgXcQ"))
        assertEquals("abc", sanitizeVideoId("ab<c>"))
    }
}
