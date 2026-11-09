package com.altomedia.smartghost.feature.diagnostics

import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.session.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        override suspend fun check(endpoint: TestEndpoint): DiagnosticResult {
            calls += endpoint.index
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

    private lateinit var checker: FakeChecker
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        checker = FakeChecker()
        repository = SessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DiagnosticsViewModel(checker, repository)

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
    fun `reload retries only failed cards`() = runTest(dispatcher) {
        val vm = viewModel()
        repository.setProxyEndpoints(
            "proxy.txt",
            listOf(proxy("good.test", 80), proxy("bad.fail", 80)),
        )
        advanceUntilIdle()

        vm.start()
        advanceUntilIdle()
        checker.calls.clear()

        vm.reloadFailed()
        advanceUntilIdle()

        assertEquals(listOf(2), checker.calls)
    }

    @Test
    fun `start without endpoints reports a note and runs nothing`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.start()
        advanceUntilIdle()

        assertTrue(checker.calls.isEmpty())
        assertEquals("Tidak ada endpoint uji yang dikonfigurasi.", vm.uiState.value.note)
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

    private fun proxy(host: String, port: Int) =
        com.altomedia.smartghost.data.proxy.ProxyEndpoint(
            host = host,
            port = port,
            scheme = "http",
            sourceLine = 1,
        )
}
