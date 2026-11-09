package com.altomedia.smartghost.data.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Proves settings actually persist and are read back — not just that the code
 * compiles.
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
        repo.setDarkTheme(false)

        assertTrue(!repo.settings.first().darkTheme)
    }
}
