package com.altomedia.smartghost.data.session

import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.proxy.ProxyEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the endpoints the user has authorised for diagnostics this session
 * (Bab 7). Home publishes the imported proxy endpoints here; the diagnostics
 * panel reads them. Singleton-scoped so both features share one source of
 * truth without depending on each other.
 */
@Singleton
class SessionRepository @Inject constructor() {

    private val _endpoints = MutableStateFlow<List<TestEndpoint>>(emptyList())
    val endpoints: StateFlow<List<TestEndpoint>> = _endpoints.asStateFlow()

    /** Every endpoint imported from proxy.txt, before any sampling. */
    private val _allEndpoints = MutableStateFlow<List<ProxyEndpoint>>(emptyList())
    val allEndpoints: StateFlow<List<ProxyEndpoint>> = _allEndpoints.asStateFlow()

    private val _proxyFileName = MutableStateFlow<String?>(null)
    val proxyFileName: StateFlow<String?> = _proxyFileName.asStateFlow()

    fun setProxyEndpoints(fileName: String, proxyEndpoints: List<ProxyEndpoint>) {
        _proxyFileName.value = fileName
        _allEndpoints.value = proxyEndpoints
        _endpoints.value = proxyEndpoints.take(MAX_SESSION_ENDPOINTS).mapIndexed { index, endpoint ->
            endpoint.toTestEndpoint(index + 1)
        }
    }

    /**
     * Picks [count] endpoints at random from the imported proxy.txt and publishes
     * them as the active session set. Called on every START so each session tests
     * a fresh random sample (Bab 7). Returns the sample so the caller can act on
     * it immediately without waiting for the flow to re-emit.
     */
    fun sampleForSession(count: Int): List<TestEndpoint> {
        val pool = _allEndpoints.value
        if (pool.isEmpty()) {
            _endpoints.value = emptyList()
            return emptyList()
        }
        val sample = pool.shuffled().take(count).mapIndexed { index, endpoint ->
            endpoint.toTestEndpoint(index + 1)
        }
        _endpoints.value = sample
        return sample
    }

    fun clear() {
        _proxyFileName.value = null
        _endpoints.value = emptyList()
        _allEndpoints.value = emptyList()
    }

    private fun ProxyEndpoint.toTestEndpoint(index: Int) = TestEndpoint(
        index = index,
        label = "$host:$port",
        url = "$scheme://$host:$port",
    )

    companion object {
        /** The panel always shows exactly ten endpoints. */
        const val MAX_SESSION_ENDPOINTS = 10
    }
}
