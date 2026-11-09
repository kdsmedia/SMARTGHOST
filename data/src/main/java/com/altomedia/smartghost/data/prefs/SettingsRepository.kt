package com.altomedia.smartghost.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** User settings that are safe to persist (Bab 9). Never secrets. */
data class UserSettings(
    val darkTheme: Boolean = true,
    val testTimeoutSeconds: Int = 8,
    val storeHistory: Boolean = false,
)

/**
 * Read-only view of settings for consumers that only need to react to changes
 * (e.g. the diagnostics panel). Keeps those consumers testable without a real
 * DataStore/Context.
 */
interface SettingsSource {
    val settings: Flow<UserSettings>
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartghost_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsSource {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val TEST_TIMEOUT = intPreferencesKey("test_timeout_seconds")
        val STORE_HISTORY = booleanPreferencesKey("store_history")
    }

    override val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            testTimeoutSeconds = prefs[Keys.TEST_TIMEOUT] ?: 8,
            storeHistory = prefs[Keys.STORE_HISTORY] ?: false,
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setTestTimeout(seconds: Int) {
        val clamped = seconds.coerceIn(2, 30)
        context.dataStore.edit { it[Keys.TEST_TIMEOUT] = clamped }
    }

    suspend fun setStoreHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STORE_HISTORY] = enabled }
    }
}
