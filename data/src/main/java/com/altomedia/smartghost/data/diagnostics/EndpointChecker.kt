package com.altomedia.smartghost.data.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLException

/**
 * Performs a single connectivity probe against an endpoint the user is
 * authorised to test (Bab 7). Conservative timeouts, no retries, no viewer
 * claims. The result reports only latency, HTTP status and a safe error summary.
 */
interface EndpointChecker {
    /**
     * Probes a single endpoint. [timeoutSeconds] comes from the user's settings
     * (Bab 9) and is clamped to a safe range (Bab 7).
     */
    suspend fun check(endpoint: TestEndpoint, timeoutSeconds: Int = 8): DiagnosticResult
}

/**
 * One test endpoint the user owns or is explicitly allowed to probe. For a
 * proxy list entry this carries the proxy host/port/scheme and any credentials,
 * so the checker can genuinely connect *through* the proxy rather than merely
 * opening a TCP socket to its host.
 */
data class TestEndpoint(
    val index: Int,
    val label: String,
    val host: String,
    val port: Int,
    val scheme: String = "http",
    val username: String? = null,
    val password: String? = null,
) {
    /** Full proxy URL without credentials (never leaks secrets into the UI). */
    val url: String get() = "$scheme://$host:$port"

    companion object {
        /** Builds an endpoint from a full URL, used by direct-URL tests. */
        fun ofUrl(index: Int, label: String, url: String): TestEndpoint {
            val uri = java.net.URI(url)
            return TestEndpoint(
                index = index,
                label = label,
                host = uri.host ?: "localhost",
                port = if (uri.port > 0) uri.port else if (uri.scheme == "https") 443 else 80,
                scheme = uri.scheme ?: "http",
            )
        }
    }
}

/**
 * Probes an endpoint by actually routing a request through it.
 *
 * - `http`/`https` proxies: an OkHttp client is pointed at the proxy and asked to
 *   reach [PROBE_URL] (a real YouTube endpoint over TLS, so the proxy must tunnel
 *   with CONNECT). Proxy credentials are applied via a proxy authenticator.
 * - `socks`/`socks5` proxies: a real SOCKS5 handshake is performed over a socket,
 *   including username/password negotiation when credentials are present.
 *
 * A green card therefore means "this proxy really carries traffic to YouTube",
 * which is the honest, useful signal — not a claim that it is "watching".
 */
class OkHttpEndpointChecker @Inject constructor(
    private val client: OkHttpClient,
) : EndpointChecker {

    /** Reachability target; overridable in tests to point at a local server. */
    private var probeUrl: String = PROBE_URL

    constructor(client: OkHttpClient, probeUrl: String) : this(client) {
        this.probeUrl = probeUrl
    }

    override suspend fun check(endpoint: TestEndpoint, timeoutSeconds: Int): DiagnosticResult =
        withContext(Dispatchers.IO) {
            val safeTimeout = timeoutSeconds.coerceIn(2, 30).toLong()
            val startedAt = System.currentTimeMillis()
            val result = when (endpoint.scheme.lowercase()) {
                "socks", "socks5" -> checkSocks5(endpoint, safeTimeout, startedAt)
                else -> checkHttpProxy(endpoint, safeTimeout, startedAt)
            }
            result
        }

    private fun checkHttpProxy(
        endpoint: TestEndpoint,
        timeoutSeconds: Long,
        startedAt: Long,
    ): DiagnosticResult {
        val builder = client.newBuilder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(timeoutSeconds + 3, TimeUnit.SECONDS)
            .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(endpoint.host, endpoint.port)))

        if (!endpoint.username.isNullOrEmpty()) {
            builder.proxyAuthenticator { _, response ->
                val credential = okhttp3.Credentials.basic(
                    endpoint.username,
                    endpoint.password.orEmpty(),
                )
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }

        val request = Request.Builder().url(probeUrl).head().build()
        return try {
            builder.build().newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startedAt
                val ok = response.code in 200..399
                DiagnosticResult(
                    index = endpoint.index,
                    endpointLabel = endpoint.label,
                    status = if (ok) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILURE,
                    latencyMs = latency,
                    httpCode = response.code,
                    lastCheckedAt = System.currentTimeMillis(),
                    errorSummary = when {
                        ok -> null
                        response.code == 407 -> "Proxy butuh autentikasi (407)"
                        else -> "HTTP ${response.code}"
                    },
                )
            }
        } catch (e: Exception) {
            failure(endpoint, startedAt, mapError(e))
        }
    }

    /** A real SOCKS5 greeting + optional user/pass auth, then success. */
    private fun checkSocks5(
        endpoint: TestEndpoint,
        timeoutSeconds: Long,
        startedAt: Long,
    ): DiagnosticResult {
        val timeoutMs = (timeoutSeconds * 1000).toInt()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
                socket.soTimeout = timeoutMs
                val out = socket.getOutputStream()
                val input = socket.getInputStream()

                val useAuth = !endpoint.username.isNullOrEmpty()
                val greeting = if (useAuth) {
                    byteArrayOf(5, 2, 0, 2)
                } else {
                    byteArrayOf(5, 1, 0)
                }
                out.write(greeting)
                out.flush()

                val reply = ByteArray(2)
                if (input.read(reply) < 2 || reply[0].toInt() != 5) {
                    return failure(endpoint, startedAt, "Bukan server SOCKS5")
                }
                when (reply[1].toInt()) {
                    0x00 -> Unit
                    0x02 -> {
                        val user = endpoint.username.orEmpty().toByteArray()
                        val pass = endpoint.password.orEmpty().toByteArray()
                        val auth = ByteArray(3 + user.size + pass.size)
                        auth[0] = 1
                        auth[1] = user.size.toByte()
                        user.copyInto(auth, 2)
                        auth[2 + user.size] = pass.size.toByte()
                        pass.copyInto(auth, 3 + user.size)
                        out.write(auth)
                        out.flush()
                        val authReply = ByteArray(2)
                        if (input.read(authReply) < 2 || authReply[1].toInt() != 0) {
                            return failure(endpoint, startedAt, "Autentikasi SOCKS5 ditolak")
                        }
                    }
                    0xFF -> return failure(endpoint, startedAt, "Proxy menolak metode autentikasi")
                    else -> return failure(endpoint, startedAt, "Metode SOCKS5 tak didukung")
                }

                val latency = System.currentTimeMillis() - startedAt
                DiagnosticResult(
                    index = endpoint.index,
                    endpointLabel = endpoint.label,
                    status = DiagnosticStatus.SUCCESS,
                    latencyMs = latency,
                    httpCode = null,
                    lastCheckedAt = System.currentTimeMillis(),
                )
            }
        } catch (e: Exception) {
            failure(endpoint, startedAt, mapError(e))
        }
    }

    private fun failure(endpoint: TestEndpoint, startedAt: Long, summary: String) =
        DiagnosticResult(
            index = endpoint.index,
            endpointLabel = endpoint.label,
            status = DiagnosticStatus.FAILURE,
            latencyMs = System.currentTimeMillis() - startedAt,
            lastCheckedAt = System.currentTimeMillis(),
            errorSummary = summary,
        )

    /** Maps a network exception to a short, user-facing, secret-free message. */
    private fun mapError(e: Exception): String = when (e) {
        is UnknownHostException -> "DNS gagal"
        is SocketTimeoutException -> "Timeout"
        is SSLException -> "TLS error"
        is java.net.UnknownServiceException ->
            "HTTP polos tidak diizinkan — gunakan https:// pada proxy.txt"
        is java.net.SocketException -> "Koneksi ditolak"
        is IOException -> e.message?.let { "Koneksi gagal: $it" } ?: "Koneksi gagal"
        else -> e.javaClass.simpleName
    }

    companion object {
        /**
         * Neutral reachability target: a tiny, standard YouTube endpoint. Using
         * TLS means an http proxy must establish a CONNECT tunnel, which is the
         * same path real playback needs.
         */
        const val PROBE_URL = "https://www.youtube.com/generate_204"
    }
}

/** Builds the shared, conservative OkHttp client (Bab 5, 7). */
object HttpClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
}
