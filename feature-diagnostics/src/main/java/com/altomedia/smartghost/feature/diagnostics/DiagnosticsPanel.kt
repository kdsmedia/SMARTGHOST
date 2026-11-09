package com.altomedia.smartghost.feature.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.smartghost.coreui.component.PrimaryActionButton

/** Stable test tag for the START button so UI tests do not depend on text. */
const val StartButtonTag = "diagnostics_start_button"

/**
 * The session console (Bab 8). START picks ten endpoints at random from the
 * imported proxy.txt and probes them; the console below lists those ten on duty
 * in a terminal style and updates live. Every line is an endpoint the device is
 * checking for connectivity — never a YouTube viewer.
 */
@Composable
fun DiagnosticsPanel(
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel Diagnostik",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "10 proxy acak dari proxy.txt · diagnostik konektivitas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::reloadFailed) {
                Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang pemeriksaan gagal")
            }
        }

        PrimaryActionButton(
            text = if (state.isRunning) "BERJALAN…" else "START",
            icon = Icons.Filled.PlayArrow,
            enabled = !state.isRunning,
            onClick = viewModel::start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag(StartButtonTag),
        )

        Spacer(Modifier.height(10.dp))

        ProxyConsole(
            cards = state.cards,
            isRunning = state.isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        state.note?.let { note ->
            Text(
                text = note,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.storeHistory) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Riwayat: ${state.history.size} baris tersimpan",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = viewModel::clearHistory,
                    enabled = state.history.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus riwayat")
                }
            }
        }

        Text(
            text = "Konsol ini menunjukkan konektivitas endpoint uji dari perangkat " +
                "Anda, bukan jumlah penonton YouTube.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
