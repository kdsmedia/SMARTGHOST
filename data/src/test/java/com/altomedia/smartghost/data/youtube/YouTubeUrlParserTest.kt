package com.altomedia.smartghost.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun `empty input returns Empty`() {
        assertEquals(UrlParseResult.Empty, YouTubeUrlParser.parse(""))
        assertEquals(UrlParseResult.Empty, YouTubeUrlParser.parse("   "))
        assertEquals(UrlParseResult.Empty, YouTubeUrlParser.parse(null))
    }

    @Test
    fun `standard watch url is parsed`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(result is UrlParseResult.Valid)
        val link = (result as UrlParseResult.Valid).link
        assertEquals("dQw4w9WgXcQ", link.videoId)
        assertEquals(YouTubeLink.Kind.WATCH, link.kind)
    }

    @Test
    fun `watch url with extra params is parsed`() {
        val result = YouTubeUrlParser.parse(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&list=PL123",
        )
        assertTrue(result is UrlParseResult.Valid)
        assertEquals("dQw4w9WgXcQ", (result as UrlParseResult.Valid).link.videoId)
    }

    @Test
    fun `youtu dot be short url is parsed`() {
        val result = YouTubeUrlParser.parse("https://youtu.be/dQw4w9WgXcQ")
        assertTrue(result is UrlParseResult.Valid)
        val link = (result as UrlParseResult.Valid).link
        assertEquals("dQw4w9WgXcQ", link.videoId)
        assertEquals(YouTubeLink.Kind.SHORT, link.kind)
    }

    @Test
    fun `shorts url is parsed`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/shorts/abcdefghijk")
        assertTrue(result is UrlParseResult.Valid)
        val link = (result as UrlParseResult.Valid).link
        assertEquals("abcdefghijk", link.videoId)
        assertEquals(YouTubeLink.Kind.SHORTS, link.kind)
    }

    @Test
    fun `live url is parsed`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/live/abcdefghijk")
        assertTrue(result is UrlParseResult.Valid)
        assertEquals(
            YouTubeLink.Kind.LIVE,
            (result as UrlParseResult.Valid).link.kind,
        )
    }

    @Test
    fun `watch url with live flag is treated as live`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/watch?v=abcdefghijk&live=1")
        assertTrue(result is UrlParseResult.Valid)
        assertEquals(
            YouTubeLink.Kind.LIVE,
            (result as UrlParseResult.Valid).link.kind,
        )
    }

    @Test
    fun `url without scheme is accepted`() {
        val result = YouTubeUrlParser.parse("youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(result is UrlParseResult.Valid)
    }

    @Test
    fun `non-youtube host is rejected`() {
        val result = YouTubeUrlParser.parse("https://vimeo.com/123456")
        assertTrue(result is UrlParseResult.NotYouTube)
        assertEquals("vimeo.com", (result as UrlParseResult.NotYouTube).host)
    }

    @Test
    fun `watch url without v parameter is invalid`() {
        val result = YouTubeUrlParser.parse("https://www.youtube.com/watch")
        assertTrue(result is UrlParseResult.InvalidVideoId)
    }

    @Test
    fun `too short video id is invalid`() {
        val result = YouTubeUrlParser.parse("https://youtu.be/short")
        assertTrue(result is UrlParseResult.InvalidVideoId)
    }

    @Test
    fun `video id with illegal characters is invalid`() {
        // 11 characters, but dots are not allowed in a YouTube id.
        val result = YouTubeUrlParser.parse("https://youtu.be/abc.def.ghi")
        assertTrue(result is UrlParseResult.InvalidVideoId)
    }

    @Test
    fun `plain text is malformed`() {
        val result = YouTubeUrlParser.parse("not a url at all !!!")
        assertTrue(result is UrlParseResult.Malformed)
    }

    @Test
    fun `youtube nocookie host is accepted`() {
        val result = YouTubeUrlParser.parse("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ")
        assertTrue(result is UrlParseResult.Valid)
    }

    @Test
    fun `canonical url is normalised for watch`() {
        val result = YouTubeUrlParser.parse("youtu.be/dQw4w9WgXcQ") as UrlParseResult.Valid
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", result.link.canonicalUrl)
    }
}
