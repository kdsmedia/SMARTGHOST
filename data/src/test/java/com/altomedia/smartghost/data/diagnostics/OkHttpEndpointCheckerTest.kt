package com.altomedia.smartghost.data.diagnostics

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

class OkHttpEndpointCheckerTest {

    private lateinit var target: MockWebServer
    private lateinit var checker: OkHttpEndpointChecker

    @Before
    fun setUp() {
        target = MockWebServer()
        target.start()
        // Route the probe at the local server so the test never touches the net.
        checker = OkHttpEndpointChecker(
            HttpClientFactory.create(),
            probeUrl = target.url("/generate_204").toString(),
        )
    }

    @After
    fun tearDown() {
        target.shutdown()
    }

    @Test
    fun `reachable proxy reports success and latency`() = runTest {
        target.enqueue(MockResponse().setResponseCode(204))
        val proxy = ForwardHttpProxy().start()
        try {
            val endpoint = TestEndpoint(1, "local", "127.0.0.1", proxy.port, "http")

            val result = checker.check(endpoint)

            assertEquals(DiagnosticStatus.SUCCESS, result.status)
            assertEquals(204, result.httpCode)
            assertTrue((result.latencyMs ?: -1) >= 0)
            assertEquals(1, target.requestCount)
        } finally {
            proxy.stop()
        }
    }

    @Test
    fun `proxy returning an error status reports failure with the code`() = runTest {
        target.enqueue(MockResponse().setResponseCode(503))
        val proxy = ForwardHttpProxy().start()
        try {
            val endpoint = TestEndpoint(2, "local", "127.0.0.1", proxy.port, "http")

            val result = checker.check(endpoint)

            assertEquals(DiagnosticStatus.FAILURE, result.status)
            assertEquals(503, result.httpCode)
            assertEquals("HTTP 503", result.errorSummary)
        } finally {
            proxy.stop()
        }
    }

    @Test
    fun `unreachable proxy is mapped to a short message`() = runTest {
        // Nothing is listening on this port.
        val dead = ServerSocket(0).use { it.localPort }
        val endpoint = TestEndpoint(3, "dead", "127.0.0.1", dead, "http")

        val result = checker.check(endpoint)

        assertEquals(DiagnosticStatus.FAILURE, result.status)
        assertTrue(result.errorSummary != null)
    }

    @Test
    fun `a plain server that is not socks5 is reported`() = runTest {
        target.enqueue(MockResponse().setResponseCode(200))
        val endpoint = TestEndpoint(4, "not-socks", "127.0.0.1", target.port, "socks5")

        val result = checker.check(endpoint)

        assertEquals(DiagnosticStatus.FAILURE, result.status)
    }

    /** A tiny forward HTTP proxy: relays one request per connection. */
    private class ForwardHttpProxy {
        private val server = ServerSocket(0)
        val port: Int get() = server.localPort
        private var thread: Thread? = null

        fun start(): ForwardHttpProxy {
            thread = Thread {
                while (!server.isClosed) {
                    val client = try {
                        server.accept()
                    } catch (e: Exception) {
                        break
                    }
                    Thread { handle(client) }.start()
                }
            }.apply { isDaemon = true; start() }
            return this
        }

        private fun handle(client: java.net.Socket) {
            client.use { c ->
                val input = c.getInputStream()
                val headerBytes = readHeaders(input) ?: return
                val requestLine = headerBytes.split("\r\n").first()
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val absolute = parts[1]
                val uri = java.net.URI(absolute)
                val host = uri.host
                val targetPort = if (uri.port > 0) uri.port else 80
                val path = (uri.rawPath ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")

                java.net.Socket(host, targetPort).use { upstream ->
                    val out = upstream.getOutputStream()
                    out.write(
                        (
                            "$method $path HTTP/1.1\r\nHost: $host:$targetPort\r\n" +
                                "Connection: close\r\n\r\n"
                            ).toByteArray(),
                    )
                    out.flush()
                    upstream.getInputStream().copyTo(c.getOutputStream())
                    c.getOutputStream().flush()
                }
            }
        }

        private fun readHeaders(input: java.io.InputStream): String? {
            val sb = StringBuilder()
            var prev = -1
            while (true) {
                val b = input.read()
                if (b == -1) return if (sb.isEmpty()) null else sb.toString()
                sb.append(b.toChar())
                if (prev == '\r'.code && b == '\n'.code && sb.endsWith("\r\n\r\n")) {
                    return sb.toString()
                }
                prev = b
            }
        }

        fun stop() {
            runCatching { server.close() }
        }
    }
}
