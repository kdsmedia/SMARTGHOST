package com.altomedia.smartghost.feature.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import com.altomedia.smartghost.data.diagnostics.HttpClientFactory
import com.altomedia.smartghost.data.diagnostics.OkHttpEndpointChecker
import com.altomedia.smartghost.data.history.DiagnosticHistoryRepository
import com.altomedia.smartghost.data.prefs.SettingsRepository
import com.altomedia.smartghost.data.proxy.ProxyEndpoint
import com.altomedia.smartghost.data.session.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Full-stack runtime test on the JVM. A real ViewModel drives the real OkHttp
 * checker against a real HTTP server and writes to a real DataStore. Nothing is
 * faked except the endpoint host. Proves the diagnostics pipeline works at
 * runtime, not merely that it compiles.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DiagnosticsIntegrationRuntimeTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Connection", "close")
                    .setBody("ok")
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun start_runs_real_http_checks_for_ten_endpoints() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = SessionRepository().apply {
            setProxyEndpoints(
                "proxy.txt",
                (1..10).map { ProxyEndpoint("127.0.0.1", server.port, "http", it) },
            )
        }
        val viewModel = DiagnosticsViewModel(
            checker = OkHttpEndpointChecker(HttpClientFactory.create()),
            sessionRepository = session,
            settingsRepository = SettingsRepository(context),
            historyRepository = DiagnosticHistoryRepository(context),
        )

        viewModel.start()

        val ok = waitFor(20_000) {
            viewModel.uiState.value.cards.all { it.status == DiagnosticStatus.SUCCESS }
        }

        assertTrue(
            "all ten cards must reach SUCCESS, got " +
                viewModel.uiState.value.cards.map { "${it.status}:${it.errorSummary}" },
            ok,
        )
        assertEquals(10, viewModel.uiState.value.cards.size)
        assertEquals(10, server.requestCount)
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return condition()
    }
}
