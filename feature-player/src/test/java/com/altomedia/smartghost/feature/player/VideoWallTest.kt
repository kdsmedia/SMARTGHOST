package com.altomedia.smartghost.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoWallTest {

    @Test
    fun `the wall always has ten cards in five columns and two rows`() {
        assertEquals(10, WALL_CARD_COUNT)
        assertEquals(5, WALL_COLUMNS)
        assertEquals(2, WALL_ROWS)

        val cards = wallCards("dQw4w9WgXcQ")
        assertEquals(10, cards.size)
    }

    @Test
    fun `every card previews the same single video, numbered 1 to 10`() {
        val cards = wallCards("dQw4w9WgXcQ")

        assertEquals((1..10).toList(), cards.map { it.index })
        assertTrue(cards.all { it.videoId == "dQw4w9WgXcQ" })
    }

    @Test
    fun `thumbnail url points at the official youtube thumbnail host`() {
        assertEquals(
            "https://img.youtube.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
            thumbnailUrl("dQw4w9WgXcQ"),
        )
    }
}
