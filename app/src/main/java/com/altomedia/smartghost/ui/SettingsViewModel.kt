package com.altomedia.smartghost.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.smartghost.BuildConfig
import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.prefs.SettingsRepository
import com.altomedia.smartghost.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: Boolean = true,
    val testTimeoutSeconds: Int = 8,
    val storeHistory: Boolean = false,
    val versionName: String = "",
    val versionCode: Int = 0,
    val proxyFileName: String? = null,
    val authorizedEndpoints: List<TestEndpoint> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        sessionRepository.endpoints,
        sessionRepository.proxyFileName,
    ) { settings, endpoints, fileName ->
        SettingsUiState(
            darkTheme = settings.darkTheme,
            testTimeoutSeconds = settings.testTimeoutSeconds,
            storeHistory = settings.storeHistory,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            proxyFileName = fileName,
            authorizedEndpoints = endpoints,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
            ),
        )

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkTheme(enabled)
    }

    fun setTestTimeout(seconds: Int) = viewModelScope.launch {
        settingsRepository.setTestTimeout(seconds)
    }

    fun setStoreHistory(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setStoreHistory(enabled)
    }
}
