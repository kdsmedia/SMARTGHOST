package com.altomedia.smartghost.feature.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.TestEndpoint
import com.altomedia.smartghost.data.history.DiagnosticHistoryStore
import com.altomedia.smartghost.data.history.HistoryEntry
import com.altomedia.smartghost.data.prefs.SettingsSource
import com.altomedia.smartghost.data.prefs.UserSettings
import com.altomedia.smartghost.data.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests (Bab 10). These exercise the real Compose panel with a
 * fake checker, proving the START button is wired to the ViewModel — the exact
 * gap that the earlier unit tests missed.
 */
@RunWith(AndroidJUnit4::class)
class DiagnosticsPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeChecker : EndpointChecker {
        val calls = mutableListOf<Int>()
        override suspend fun check(endpoint: TestEndpoint, timeoutSeconds: Int): DiagnosticResult {
            calls += endpoint.index
            return DiagnosticResult(
                index = endpoint.index,
                endpointLabel = endpoint.label,
                status = DiagnosticStatus.SUCCESS,
                latencyMs = 7,
                httpCode = 200,
            )
        }
    }

    private class FakeSettings : SettingsSource {
        override val settings: Flow<UserSettings> = MutableStateFlow(UserSettings())
    }

    private class FakeHistory : DiagnosticHistoryStore {
        private val store = MutableStateFlow<List<HistoryEntry>>(emptyList())
        override val history: Flow<List<HistoryEntry>> = store
        override suspend fun add(entry: HistoryEntry) {
            store.value = listOf(entry) + store.value
        }
        override suspend fun clear() {
            store.value = emptyList()
        }
    }

    @Test
    fun start_button_is_visible_and_triggers_checks() {
        val checker = FakeChecker()
        val session = SessionRepository().apply {
            setProxyEndpoints(
                "proxy.txt",
                listOf(
                    com.altomedia.smartghost.data.proxy.ProxyEndpoint("a.test", 80, "http", 1),
                    com.altomedia.smartghost.data.proxy.ProxyEndpoint("b.test", 80, "http", 2),
                ),
            )
        }
        val viewModel = DiagnosticsViewModel(checker, session, FakeSettings(), FakeHistory())

        composeRule.setContent {
            DiagnosticsPanel(viewModel = viewModel)
        }

        composeRule.onNodeWithTag(StartButtonTag).assertIsDisplayed()
        composeRule.onNodeWithTag(StartButtonTag).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(1, 2), checker.calls.sorted())
        composeRule.onNodeWithText("Panel Diagnostik").assertIsDisplayed()
    }
}
