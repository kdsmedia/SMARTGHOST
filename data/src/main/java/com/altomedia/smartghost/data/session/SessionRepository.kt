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

    private val _proxyFileName = MutableStateFlow<String?>(null)
    val proxyFileName: StateFlow<String?> = _proxyFileName.asStateFlow()

    fun setProxyEndpoints(fileName: String, proxyEndpoints: List<ProxyEndpoint>) {
        _proxyFileName.value = fileName
        _endpoints.value = proxyEndpoints.mapIndexed { index, endpoint ->
            TestEndpoint(
                index = index + 1,
                label = "${endpoint.host}:${endpoint.port}",
                url = "${endpoint.scheme}://${endpoint.host}:${endpoint.port}",
            )
        }
    }

    fun clear() {
        _proxyFileName.value = null
        _endpoints.value = emptyList()
    }
}
