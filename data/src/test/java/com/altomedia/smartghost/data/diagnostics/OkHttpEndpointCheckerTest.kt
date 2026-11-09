package com.altomedia.smartghost.data.diagnostics

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpEndpointCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var checker: OkHttpEndpointChecker

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        checker = OkHttpEndpointChecker(HttpClientFactory.create())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful endpoint reports success and latency`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val endpoint = TestEndpoint(1, "local", server.url("/health").toString())

        val result = checker.check(endpoint)

        assertEquals(DiagnosticStatus.SUCCESS, result.status)
        assertEquals(200, result.httpCode)
        assertTrue((result.latencyMs ?: -1) >= 0)
    }

    @Test
    fun `server error reports failure with http code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val endpoint = TestEndpoint(2, "local", server.url("/health").toString())

        val result = checker.check(endpoint)

        assertEquals(DiagnosticStatus.FAILURE, result.status)
        assertEquals(503, result.httpCode)
        assertEquals("HTTP 503", result.errorSummary)
    }

    @Test
    fun `dns failure is mapped to a short message`() = runTest {
        val endpoint = TestEndpoint(
            3,
            "unreachable",
            "http://this-host-does-not-exist-smartghost.invalid/health",
        )

        val result = checker.check(endpoint)

        assertEquals(DiagnosticStatus.FAILURE, result.status)
        assertEquals("DNS gagal", result.errorSummary)
    }
}
