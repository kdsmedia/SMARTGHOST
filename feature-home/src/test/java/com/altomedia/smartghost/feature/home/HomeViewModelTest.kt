package com.altomedia.smartghost.feature.home

import com.altomedia.smartghost.data.session.SessionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    private val sessionRepository = SessionRepository()
    private val viewModel = HomeViewModel(sessionRepository)

    @Test
    fun `loading a proxy file publishes endpoints to the session`() {
        viewModel.onProxyFileLoaded("proxy.txt", "1.2.3.4:8080\n5.6.7.8:9090")

        val state = viewModel.uiState.value
        assertEquals("proxy.txt", state.proxyFileName)
        assertEquals(2, state.proxyEndpointCount)
        assertTrue(state.hasProxyLoaded)
        assertEquals(2, sessionRepository.endpoints.value.size)
        assertNotNull(state.message)
    }

    @Test
    fun `proxy file with utf8 bom still parses endpoints`() {
        viewModel.onProxyFileLoaded("proxy.txt", "\uFEFF10.0.0.1:1080")

        val state = viewModel.uiState.value
        assertEquals(1, state.proxyEndpointCount)
        assertTrue(state.proxyErrors.isEmpty())
    }

    @Test
    fun `unreadable proxy file reports a clear message and clears session`() {
        viewModel.onProxyFileLoaded("proxy.txt", "1.2.3.4:8080")
        viewModel.onProxyFileLoaded("broken.txt", null)

        val state = viewModel.uiState.value
        assertEquals("broken.txt", state.proxyFileName)
        assertEquals(0, state.proxyEndpointCount)
        assertTrue(state.message!!.contains("tidak dapat dibaca"))
        assertTrue(sessionRepository.endpoints.value.isEmpty())
    }

    @Test
    fun `clearProxy empties state and session`() {
        viewModel.onProxyFileLoaded("proxy.txt", "1.2.3.4:8080")
        viewModel.clearProxy()

        val state = viewModel.uiState.value
        assertNull(state.proxyFileName)
        assertEquals(0, state.proxyEndpointCount)
        assertTrue(sessionRepository.endpoints.value.isEmpty())
    }
}
