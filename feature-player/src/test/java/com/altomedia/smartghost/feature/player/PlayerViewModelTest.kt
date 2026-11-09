package com.altomedia.smartghost.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerViewModelTest {

    private val viewModel = PlayerViewModel()

    @Test
    fun `load starts in the loading state for the given video`() {
        viewModel.load("dQw4w9WgXcQ")

        val state = viewModel.uiState.value
        assertEquals("dQw4w9WgXcQ", state.videoId)
        assertEquals(PlayerStatus.LOADING, state.status)
        assertNull(state.lastError)
    }

    @Test
    fun `state codes map to the right labels`() {
        viewModel.load("dQw4w9WgXcQ")

        viewModel.onPlaybackStateChanged(1)
        assertEquals(PlayerStatus.PLAYING, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.onPlaybackStateChanged(2)
        assertEquals(PlayerStatus.PAUSED, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.onPlaybackStateChanged(3)
        assertEquals(PlayerStatus.BUFFERING, viewModel.uiState.value.status)

        viewModel.onPlaybackStateChanged(0)
        assertEquals(PlayerStatus.ENDED, viewModel.uiState.value.status)

        viewModel.onPlaybackStateChanged(5)
        assertEquals(PlayerStatus.READY, viewModel.uiState.value.status)
    }

    @Test
    fun `unknown state codes are ignored`() {
        viewModel.load("dQw4w9WgXcQ")
        viewModel.onPlaybackStateChanged(1)
        viewModel.onPlaybackStateChanged(42)

        assertEquals(PlayerStatus.PLAYING, viewModel.uiState.value.status)
    }

    @Test
    fun `an error moves the player to the error state with a message`() {
        viewModel.load("dQw4w9WgXcQ")
        viewModel.onError("Video tidak ditemukan atau bersifat privat.")

        val state = viewModel.uiState.value
        assertEquals(PlayerStatus.ERROR, state.status)
        assertEquals("Video tidak ditemukan atau bersifat privat.", state.lastError)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `reloading clears a previous error`() {
        viewModel.load("dQw4w9WgXcQ")
        viewModel.onError("gagal")
        viewModel.load("dQw4w9WgXcQ")

        assertNull(viewModel.uiState.value.lastError)
        assertEquals(PlayerStatus.LOADING, viewModel.uiState.value.status)
    }
}
