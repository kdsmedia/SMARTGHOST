package com.altomedia.smartghost.feature.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Honest playback status reported by YouTube's own player. Never a viewer count. */
enum class PlayerStatus(val label: String) {
    LOADING("Memuat…"),
    READY("Siap diputar"),
    PLAYING("Sedang diputar"),
    PAUSED("Jeda"),
    BUFFERING("Menyangga…"),
    ENDED("Selesai"),
    ERROR("Gagal"),
}

/** Player UI state for a single, user-initiated official playback session. */
data class PlayerUiState(
    val videoId: String = "",
    val status: PlayerStatus = PlayerStatus.LOADING,
    val lastError: String? = null,
) {
    val isPlaying: Boolean get() = status == PlayerStatus.PLAYING
}

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun load(videoId: String) {
        _uiState.value = PlayerUiState(videoId = videoId, status = PlayerStatus.LOADING)
    }

    /** Maps an IFrame API state code (see the YouTube IFrame API reference). */
    fun onPlaybackStateChanged(stateCode: Int) {
        val status = when (stateCode) {
            -1 -> PlayerStatus.READY
            0 -> PlayerStatus.ENDED
            1 -> PlayerStatus.PLAYING
            2 -> PlayerStatus.PAUSED
            3 -> PlayerStatus.BUFFERING
            5 -> PlayerStatus.READY
            else -> return
        }
        _uiState.update { it.copy(status = status, lastError = null) }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(status = PlayerStatus.ERROR, lastError = message) }
    }
}
