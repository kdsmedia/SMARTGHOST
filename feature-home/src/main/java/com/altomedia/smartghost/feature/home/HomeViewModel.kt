package com.altomedia.smartghost.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.smartghost.data.proxy.ProxyFileParser
import com.altomedia.smartghost.data.proxy.ProxyParseResult
import com.altomedia.smartghost.data.session.SessionRepository
import com.altomedia.smartghost.data.youtube.UrlParseResult
import com.altomedia.smartghost.data.youtube.YouTubeUrlParser
import com.altomedia.smartghost.security.SecretRedactor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _startRequests = Channel<StartSessionRequest>(Channel.BUFFERED)
    val startRequests = _startRequests.receiveAsFlow()

    private var lastProxy: ProxyParseResult? = null

    fun onUrlChanged(value: String) {
        val result = YouTubeUrlParser.parse(value)
        _uiState.update { current ->
            current.copy(
                urlInput = value,
                urlValidation = result.toValidation(),
                canStart = result is UrlParseResult.Valid,
                message = null,
            )
        }
    }

    /**
     * Handles the text content of a proxy.txt file chosen by the user. Only
     * diagnostics against the user's own/authorised endpoints are supported
     * (Bab 7) — this never configures YouTube viewer spoofing.
     */
    fun onProxyFileLoaded(fileName: String, content: String?) {
        val result = ProxyFileParser.parse(content)
        lastProxy = result
        sessionRepository.setProxyEndpoints(fileName, result.endpoints)
        _uiState.update { current ->
            current.copy(
                proxyFileName = fileName,
                proxyEndpointCount = result.endpoints.size,
                proxyErrors = result.errors,
                message = buildProxyMessage(result),
            )
        }
    }

    fun clearProxy() {
        lastProxy = null
        sessionRepository.clear()
        _uiState.update {
            it.copy(proxyFileName = null, proxyEndpointCount = 0, proxyErrors = emptyList())
        }
    }

    fun onStart() {
        val parsed = YouTubeUrlParser.parse(_uiState.value.urlInput)
        if (parsed !is UrlParseResult.Valid) {
            _uiState.update { it.copy(urlValidation = parsed.toValidation()) }
            return
        }
        viewModelScope.launch {
            _startRequests.send(
                StartSessionRequest(
                    canonicalUrl = parsed.link.canonicalUrl,
                    videoId = parsed.link.videoId,
                ),
            )
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun buildProxyMessage(result: ProxyParseResult): String? {
        val parts = mutableListOf<String>()
        if (result.endpoints.isNotEmpty()) {
            parts += "${result.endpoints.size} endpoint valid dimuat."
        }
        if (result.errors.isNotEmpty()) {
            parts += "${result.errors.size} baris bermasalah."
        }
        if (result.duplicateCount > 0) {
            parts += "${result.duplicateCount} duplikat diabaikan."
        }
        if (parts.isEmpty()) {
            parts += "File proxy tidak berisi endpoint yang valid."
        }
        return SecretRedactor.redact(parts.joinToString(" "))
    }
}
