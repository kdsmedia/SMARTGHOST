package com.altomedia.smartghost.feature.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.smartghost.coreui.component.PrimaryActionButton
import com.altomedia.smartghost.coreui.component.StatusLevel
import com.altomedia.smartghost.coreui.component.StatusPill
import com.altomedia.smartghost.coreui.component.SmartCard
import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus

/** Stable test tag for the START button so UI tests do not depend on text. */
const val StartButtonTag = "diagnostics_start_button"

/**
 * Panel of ten diagnostic cards (Bab 8). Each card reports the status of one
 * authorised endpoint test — connectivity, latency, last check and a safe error
 * summary. It explicitly never claims to represent a YouTube viewer.
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
                    text = "10 kartu status · uji endpoint yang diizinkan",
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

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.cards, key = { it.index }) { card ->
                DiagnosticCard(card)
            }
        }

        if (state.note != null) {
            Text(
                text = state.note!!,
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
            text = "Status ini menunjukkan konektivitas endpoint uji, bukan jumlah " +
                "penonton YouTube.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiagnosticCard(card: DiagnosticResult) {
    SmartCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${card.index}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(6.dp))
                StatusPill(level = card.status.toLevel(), label = card.status.label())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = card.endpointLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = card.latencyMs?.let { "$it ms" } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            card.errorSummary?.let { summary ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

internal fun DiagnosticStatus.toLevel(): StatusLevel = when (this) {
    DiagnosticStatus.NOT_TESTED -> StatusLevel.NEUTRAL
    DiagnosticStatus.RUNNING -> StatusLevel.RUNNING
    DiagnosticStatus.SUCCESS -> StatusLevel.SUCCESS
    DiagnosticStatus.FAILURE -> StatusLevel.FAILURE
}

internal fun DiagnosticStatus.label(): String = when (this) {
    DiagnosticStatus.NOT_TESTED -> "Belum diuji"
    DiagnosticStatus.RUNNING -> "Berjalan"
    DiagnosticStatus.SUCCESS -> "Berhasil"
    DiagnosticStatus.FAILURE -> "Gagal"
}
