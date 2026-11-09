package com.altomedia.smartghost.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/** Exactly ten diagnostic cards (Bab 8). */
const val CARD_COUNT = 10

/** Conservative concurrency so we never flood endpoints (Bab 7). */
private const val MAX_CONCURRENCY = 3

data class DiagnosticsUiState(
    val cards: List<DiagnosticResult> = defaultCards(),
    val isRunning: Boolean = false,
    val proxyFileName: String? = null,
    val note: String? = null,
) {
    companion object {
        fun defaultCards(): List<DiagnosticResult> = (1..CARD_COUNT).map { index ->
            DiagnosticResult.empty(index, label = "Endpoint $index")
        }
    }
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val checker: EndpointChecker,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    /** Endpoints the user is authorised to test, one per card (max ten). */
    private var endpoints: List<TestEndpoint> = emptyList()

    init {
        viewModelScope.launch {
            sessionRepository.endpoints.collect { imported ->
                endpoints = imported.take(CARD_COUNT)
                applyEndpoints()
            }
        }
        viewModelScope.launch {
            sessionRepository.proxyFileName.collect { name ->
                _uiState.update { it.copy(proxyFileName = name) }
            }
        }
    }

    private fun applyEndpoints() {
        _uiState.update { current ->
            val cards = (1..CARD_COUNT).map { index ->
                val ep = endpoints.getOrNull(index - 1)
                DiagnosticResult.empty(index, label = ep?.label ?: "Endpoint $index")
            }
            current.copy(cards = cards, isRunning = false)
        }
    }

    fun setEndpoints(testEndpoints: List<TestEndpoint>) {
        endpoints = testEndpoints.take(CARD_COUNT)
        applyEndpoints()
    }

    fun setProxyFileName(name: String?) {
        _uiState.update { it.copy(proxyFileName = name) }
    }

    /** START: run one diagnostic pass with bounded concurrency (Bab 8). */
    fun start() {
        if (endpoints.isEmpty()) {
            _uiState.update { it.copy(note = "Tidak ada endpoint uji yang dikonfigurasi.") }
            return
        }
        runChecks(endpoints)
    }

    /** RELOAD: retry only the cards that failed; never replays playback. */
    fun reloadFailed() {
        val failedIndexes = _uiState.value.cards
            .filter { it.status == DiagnosticStatus.FAILURE }
            .map { it.index }
            .toSet()

        val toRetry = endpoints.filter { it.index in failedIndexes }
        val targets = toRetry.ifEmpty { endpoints }
        runChecks(targets)
    }

    /** HOME: cancel any running diagnostics safely (Bab 8). */
    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
        _uiState.update { current ->
            current.copy(
                isRunning = false,
                cards = current.cards.map { card ->
                    if (card.status == DiagnosticStatus.RUNNING) {
                        card.copy(status = DiagnosticStatus.NOT_TESTED)
                    } else {
                        card
                    }
                },
            )
        }
    }

    private fun runChecks(targets: List<TestEndpoint>) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isRunning = true,
                    note = null,
                    cards = current.cards.map { card ->
                        if (targets.any { it.index == card.index }) {
                            card.copy(status = DiagnosticStatus.RUNNING, errorSummary = null)
                        } else {
                            card
                        }
                    },
                )
            }

            val semaphore = Semaphore(MAX_CONCURRENCY)
            targets.map { endpoint ->
                launch {
                    semaphore.withPermit {
                        val result = checker.check(endpoint)
                        updateCard(result)
                    }
                }
            }.forEach { it.join() }

            _uiState.update { it.copy(isRunning = false) }
        }
    }

    private fun updateCard(result: DiagnosticResult) {
        _uiState.update { current ->
            current.copy(
                cards = current.cards.map { card ->
                    if (card.index == result.index) result else card
                },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
    }
}
