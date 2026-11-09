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
 * Runtime proof that the player host page is served from a real https origin.
 *
 * The YouTube IFrame player refuses to start (error 153) when its host document
 * sends no Referer, which is what happens when the page is loaded with
 * `loadDataWithBaseURL` (a `data:` origin). These tests exercise the real
 * [WebViewAssetLoader] to confirm the bundled page is reachable over
 * `https://appassets.androidplatform.net` and carries a referrer policy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlayerAssetRuntimeTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `player url uses the https asset origin`() {
        val url = playerUrl("dQw4w9WgXcQ")
        assertTrue(url.startsWith("$PLAYER_ORIGIN/assets/player.html"))
        assertTrue(url.contains("videoId=dQw4w9WgXcQ"))
    }

    @Test
    fun `asset loader serves the bundled player page`() {
        val loader = playerAssetLoader(context)
        val url = android.net.Uri.parse(playerUrl("dQw4w9WgXcQ"))

        val response: WebResourceResponse? = loader.shouldInterceptRequest(url)

        assertNotNull("player.html must be served from assets", response)
        val html = response!!.data.bufferedReader().use { it.readText() }
        assertTrue(html.contains("youtube.com/iframe_api"))
        assertTrue(html.contains("referrer"))
        assertTrue(html.contains("onYouTubeIframeAPIReady"))
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
