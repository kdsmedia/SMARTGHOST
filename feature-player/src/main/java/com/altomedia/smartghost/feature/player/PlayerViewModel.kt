package com.altomedia.smartghost.feature.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Player UI state for a single, user-initiated official playback session. */
data class PlayerUiState(
    val videoId: String = "",
    val title: String = "Sesi pemutaran resmi",
    val isPlaying: Boolean = false,
    val lastError: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun load(videoId: String) {
        _uiState.update { it.copy(videoId = videoId, lastError = null) }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(isPlaying = false, lastError = message) }
    }
}
