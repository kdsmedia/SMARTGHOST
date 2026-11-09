package com.altomedia.smartghost.data.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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

/** A test endpoint that the user owns or is explicitly allowed to probe. */
data class TestEndpoint(
    val index: Int,
    val label: String,
    val url: String,
)

class OkHttpEndpointChecker @Inject constructor(
    private val client: OkHttpClient,
) : EndpointChecker {

    override suspend fun check(endpoint: TestEndpoint, timeoutSeconds: Int): DiagnosticResult =
        withContext(Dispatchers.IO) {
            val safeTimeout = timeoutSeconds.coerceIn(2, 30).toLong()
            val requestClient = client.newBuilder()
                .connectTimeout(safeTimeout, TimeUnit.SECONDS)
                .readTimeout(safeTimeout, TimeUnit.SECONDS)
                .callTimeout(safeTimeout + 3, TimeUnit.SECONDS)
                .build()

            val startedAt = System.currentTimeMillis()
            val request = Request.Builder()
                .url(endpoint.url)
                .head()
                .build()

            try {
                requestClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startedAt
                    val success = response.isSuccessful
                    DiagnosticResult(
                        index = endpoint.index,
                        endpointLabel = endpoint.label,
                        status = if (success) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILURE,
                        latencyMs = latency,
                        httpCode = response.code,
                        lastCheckedAt = System.currentTimeMillis(),
                        errorSummary = if (success) null else "HTTP ${response.code}",
                    )
                }
            } catch (e: Exception) {
                DiagnosticResult(
                    index = endpoint.index,
                    endpointLabel = endpoint.label,
                    status = DiagnosticStatus.FAILURE,
                    latencyMs = System.currentTimeMillis() - startedAt,
                    lastCheckedAt = System.currentTimeMillis(),
                    errorSummary = mapError(e),
                )
            }
        }

    /** Maps a network exception to a short, user-facing, secret-free message. */
    private fun mapError(e: Exception): String = when (e) {
        is UnknownHostException -> "DNS gagal"
        is SocketTimeoutException -> "Timeout"
        is SSLException -> "TLS error"
        is IOException -> e.message?.let { "Koneksi gagal: $it" } ?: "Koneksi gagal"
        else -> e.javaClass.simpleName
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
