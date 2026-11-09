package com.altomedia.smartghost.feature.diagnostics

import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.history.DiagnosticHistoryStore
import com.altomedia.smartghost.data.history.HistoryEntry
import com.altomedia.smartghost.data.prefs.SettingsSource
import com.altomedia.smartghost.data.prefs.UserSettings
import com.altomedia.smartghost.data.session.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Deterministic checker: any endpoint whose url contains "fail" fails. */
    private class FakeChecker : EndpointChecker {
        var calls = mutableListOf<Int>()
        var lastLabels = mutableListOf<String>()
        var lastTimeout: Int? = null
        override suspend fun check(endpoint: TestEndpoint, timeoutSeconds: Int): DiagnosticResult {
            calls += endpoint.index
            lastLabels += endpoint.label
            lastTimeout = timeoutSeconds
            val success = !endpoint.url.contains("fail")
            return DiagnosticResult(
                index = endpoint.index,
                endpointLabel = endpoint.label,
                status = if (success) DiagnosticStatus.SUCCESS else DiagnosticStatus.FAILURE,
                latencyMs = 12,
                httpCode = if (success) 200 else 500,
                errorSummary = if (success) null else "HTTP 500",
            )
        }
    }

    private class FakeSettings(initial: UserSettings = UserSettings()) : SettingsSource {
        override val settings: Flow<UserSettings> = MutableStateFlow(initial)
    }

    private class FakeHistory : DiagnosticHistoryStore {
        val store = MutableStateFlow<List<HistoryEntry>>(emptyList())
        override val history: Flow<List<HistoryEntry>> = store
        override suspend fun add(entry: HistoryEntry) {
            store.value = listOf(entry) + store.value
        }
        override suspend fun clear() {
            store.value = emptyList()
        }
    }

    private lateinit var checker: FakeChecker
    private lateinit var repository: SessionRepository
    private lateinit var settings: FakeSettings
    private lateinit var history: FakeHistory

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        checker = FakeChecker()
        repository = SessionRepository()
        settings = FakeSettings()
        history = FakeHistory()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DiagnosticsViewModel(checker, repository, settings, history)

    @Test
    fun `panel always exposes ten cards`() {
        val vm = viewModel()
        assertEquals(10, vm.uiState.value.cards.size)
        assertTrue(vm.uiState.value.cards.all { it.status == DiagnosticStatus.NOT_TESTED })
    }

    @Test
    fun `imported endpoints populate card labels`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints(
            "proxy.txt",
            listOf(
                proxy("127.0.0.1", 8080),
                proxy("10.0.0.1", 3128),
            ),
        )
        advanceUntilIdle()

        assertEquals("127.0.0.1:8080", vm.uiState.value.cards[0].endpointLabel)
        assertEquals("10.0.0.1:3128", vm.uiState.value.cards[1].endpointLabel)
        assertEquals("proxy.txt", vm.uiState.value.proxyFileName)
    }

    @Test
    fun `start runs checks for all imported endpoints`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints(
            "proxy.txt",
            listOf(proxy("a.test", 80), proxy("b.test", 80)),
        )
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), checker.calls.sorted())
        assertEquals(DiagnosticStatus.SUCCESS, vm.uiState.value.cards[0].status)
        assertEquals(DiagnosticStatus.SUCCESS, vm.uiState.value.cards[1].status)
        assertEquals(DiagnosticStatus.NOT_TESTED, vm.uiState.value.cards[2].status)
    }

    @Test
    fun `reload retries only the endpoint that failed`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints(
            "proxy.txt",
            listOf(proxy("good.test", 80), proxy("bad.fail", 80)),
        )
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()
        checker.calls.clear()
        checker.lastLabels.clear()

        vm.reloadFailed()
        advanceUntilIdle()

        assertEquals(listOf("bad.fail:80"), checker.lastLabels)
    }

    @Test
    fun `start without endpoints reports a note and runs nothing`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()

        assertTrue(checker.calls.isEmpty())
        assertEquals(
            "Tidak ada endpoint uji. Impor file proxy.txt lebih dulu.",
            vm.uiState.value.note,
        )
    }

    @Test
    fun `start samples exactly ten endpoints from a larger pool`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints(
            "proxy.txt",
            (1..40).map { proxy("10.0.0.$it", 8000 + it) },
        )
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()

        assertEquals((1..10).toList(), checker.calls.sorted())
        val labels = vm.uiState.value.cards.map { it.endpointLabel }
        assertEquals(10, labels.toSet().size)
        assertTrue(vm.uiState.value.cards.all { it.status == DiagnosticStatus.SUCCESS })
    }

    @Test
    fun `stop cancels running session`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints("proxy.txt", listOf(proxy("a.test", 80)))
        advanceUntilIdle()

        vm.start()
        vm.stop()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isRunning)
    }

    @Test
    fun `settings timeout is passed to the checker`() = runTest(dispatcher) {
        settings = FakeSettings(UserSettings(testTimeoutSeconds = 15))
        val vm = viewModel()
        repository.setProxyEndpoints("proxy.txt", listOf(proxy("a.test", 80)))
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()

        assertEquals(15, checker.lastTimeout)
    }

    @Test
    fun `history is recorded only when enabled and can be cleared`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints("proxy.txt", listOf(proxy("a.test", 80)))
        advanceUntilIdle()

        // Disabled by default: nothing stored.
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.history.isEmpty())

        // Enable history and run again with a fresh ViewModel.
        settings = FakeSettings(UserSettings(storeHistory = true))
        val vm2 = DiagnosticsViewModel(checker, repository, settings, history)
        advanceUntilIdle()
        vm2.start()
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.history.isNotEmpty())

        vm2.clearHistory()
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.history.isEmpty())
    }

    private fun proxy(host: String, port: Int) =
        com.altomedia.smartghost.data.proxy.ProxyEndpoint(
            host = host,
            port = port,
            scheme = "http",
            sourceLine = 1,
        )
}
