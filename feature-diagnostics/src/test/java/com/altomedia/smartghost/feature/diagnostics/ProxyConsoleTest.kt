package com.altomedia.smartghost.feature.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Pure-JVM checks for the console clock. */
class ProxyConsoleFormatTest {
    @Test
    fun `clock formats minutes and seconds`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("00:09", formatElapsed(9))
        assertEquals("01:05", formatElapsed(65))
        assertEquals("59:59", formatElapsed(3599))
    }

    @Test
    fun `clock clamps negative values`() {
        assertEquals("00:00", formatElapsed(-5))
    }
}

/** Runtime proof the console really renders the ten endpoints it is given. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ProxyConsoleRuntimeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun console_lists_ten_endpoints_and_running_header() {
        val cards = (1..10).map { index ->
            DiagnosticResult(
                index = index,
                endpointLabel = "10.0.0.$index:808$index",
                status = if (index == 1) DiagnosticStatus.RUNNING else DiagnosticStatus.NOT_TESTED,
            )
        }

        composeRule.setContent { ProxyConsole(cards = cards, isRunning = true) }

        composeRule.onNodeWithText("10.0.0.1:8081").assertIsDisplayed()
        composeRule.onNodeWithText("10.0.0.10:80810").assertExists()
        composeRule.onNodeWithText("● LIVE").assertIsDisplayed()
        composeRule.onNodeWithText("1/10 proxy bertugas").assertIsDisplayed()
    }
}
