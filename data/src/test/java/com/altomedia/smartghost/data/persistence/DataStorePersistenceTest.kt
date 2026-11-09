package com.altomedia.smartghost.data.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altomedia.smartghost.data.history.DiagnosticHistoryRepository
import com.altomedia.smartghost.data.history.HistoryEntry
import com.altomedia.smartghost.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Runtime test against a REAL DataStore on a Robolectric Android context.
 * Proves settings and history actually persist and are read back — not just
 * that the code compiles.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DataStorePersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun settings_are_written_and_read_back() = runTest {
        val repo = SettingsRepository(context)
        repo.setTestTimeout(15)
        repo.setStoreHistory(true)

        val settings = repo.settings.first()
        assertEquals(15, settings.testTimeoutSeconds)
        assertTrue(settings.storeHistory)
    }

    @Test
    fun test_timeout_is_clamped_to_safe_range() = runTest {
        val repo = SettingsRepository(context)
        repo.setTestTimeout(999)
        assertEquals(30, repo.settings.first().testTimeoutSeconds)
        repo.setTestTimeout(0)
        assertEquals(2, repo.settings.first().testTimeoutSeconds)
    }

    @Test
    fun history_round_trips_and_clears() = runTest {
        val repo = DiagnosticHistoryRepository(context)
        repo.clear()

        repo.add(HistoryEntry(1_000L, "a.test:80", "SUCCESS", "200 OK"))
        repo.add(HistoryEntry(2_000L, "b.test:80", "FAILED", "Timeout"))

        val entries = repo.history.first()
        assertEquals(2, entries.size)
        assertEquals("b.test:80", entries[0].label)

        repo.clear()
        assertEquals(0, repo.history.first().size)
    }

    @Test
    fun history_redacts_secrets_before_storage() = runTest {
        val repo = DiagnosticHistoryRepository(context)
        repo.clear()
        repo.add(
            HistoryEntry(
                timestamp = 5L,
                label = "endpoint",
                status = "FAILED",
                detail = "Authorization: Bearer ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
            ),
        )
        val stored = repo.history.first().first().detail
        assertTrue("token must be redacted", !stored.contains("ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"))
    }
}
