package com.altomedia.smartghost.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.smartghost.data.youtube.UrlParseResult
import com.altomedia.smartghost.data.youtube.YouTubeUrlParser
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
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _startRequests = Channel<StartSessionRequest>(Channel.BUFFERED)
    val startRequests = _startRequests.receiveAsFlow()

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
}
