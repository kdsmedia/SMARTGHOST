package com.altomedia.smartghost.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.altomedia.smartghost.security.SecretRedactor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** One stored diagnostic line (Bab 9). Never contains secrets. */
data class HistoryEntry(
    val timestamp: Long,
    val label: String,
    val status: String,
    val detail: String,
)

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "smartghost_history")

/**
 * Read-only + write view of diagnostic history for consumers. Lets the
 * diagnostics panel be tested without a real DataStore/Context.
 */
interface DiagnosticHistoryStore {
    val history: Flow<List<HistoryEntry>>
    suspend fun add(entry: HistoryEntry)
    suspend fun clear()
}

/**
 * Optional diagnostic history (Bab 9). Only written when the user enables
 * "Simpan riwayat diagnostik"; the user can clear it at any time. Entries are
 * redacted before storage and capped so the store never grows unbounded.
 */
@Singleton
class DiagnosticHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiagnosticHistoryStore {
    private object Keys {
        val ENTRIES = stringPreferencesKey("entries")
    }

    override val history: Flow<List<HistoryEntry>> = context.historyStore.data.map { prefs ->
        decode(prefs[Keys.ENTRIES].orEmpty())
    }

    override suspend fun add(entry: HistoryEntry) {
        context.historyStore.edit { prefs ->
            val current = decode(prefs[Keys.ENTRIES].orEmpty())
            val updated = (listOf(entry) + current).take(MAX_ENTRIES)
            prefs[Keys.ENTRIES] = encode(updated)
        }
    }

    override suspend fun clear() {
        context.historyStore.edit { it.remove(Keys.ENTRIES) }
    }

    private fun encode(entries: List<HistoryEntry>): String =
        entries.joinToString("\n") { entry ->
            listOf(
                entry.timestamp.toString(),
                entry.label.replace(FIELD_SEP, ' '),
                entry.status,
                entry.detail.replace(FIELD_SEP, ' '),
            ).joinToString(FIELD_SEP.toString())
        }

    private fun decode(raw: String): List<HistoryEntry> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(FIELD_SEP)
                if (parts.size != 4) return@mapNotNull null
                val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
                HistoryEntry(
                    timestamp = ts,
                    label = SecretRedactor.redact(parts[1]),
                    status = parts[2],
                    detail = SecretRedactor.redact(parts[3]),
                )
            }
            .toList()

    private companion object {
        const val FIELD_SEP = '\u001F'
        const val MAX_ENTRIES = 200
    }
}
