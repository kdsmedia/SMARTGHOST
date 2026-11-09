package com.altomedia.smartghost.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val viewModel = HomeViewModel()

    @Test
    fun `empty input cannot start`() {
        assertEquals("", viewModel.uiState.value.urlInput)
        assertFalse(viewModel.uiState.value.canStart)
    }

    @Test
    fun `valid youtube url enables start`() {
        viewModel.onUrlChanged("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        val state = viewModel.uiState.value
        assertTrue(state.canStart)
        assertEquals(UrlValidation.Valid, state.urlValidation)
    }

    @Test
    fun `non youtube url is rejected`() {
        viewModel.onUrlChanged("https://vimeo.com/123456")

        val state = viewModel.uiState.value
        assertFalse(state.canStart)
        assertTrue(state.urlValidation is UrlValidation.Invalid)
    }

    @Test
    fun `malformed url is rejected`() {
        viewModel.onUrlChanged("not a url at all !!!")

        val state = viewModel.uiState.value
        assertFalse(state.canStart)
        assertTrue(state.urlValidation is UrlValidation.Invalid)
    }

    @Test
    fun `clearing the url returns to idle`() {
        viewModel.onUrlChanged("https://youtu.be/dQw4w9WgXcQ")
        viewModel.onUrlChanged("")

        val state = viewModel.uiState.value
        assertFalse(state.canStart)
        assertEquals(UrlValidation.Idle, state.urlValidation)
    }
}
