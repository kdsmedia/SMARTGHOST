package com.altomedia.smartghost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.altomedia.smartghost.coreui.component.SecondaryActionButton
import com.altomedia.smartghost.feature.diagnostics.DiagnosticsPanel
import com.altomedia.smartghost.feature.diagnostics.DiagnosticsViewModel
import com.altomedia.smartghost.feature.player.PlayerScreen

/**
 * Session screen (Bab 4): one official player plus the ten-card diagnostic
 * panel, with HOME and RELOAD controls in a bottom bar (Bab 8).
 *
 * The [DiagnosticsViewModel] is hoisted here so the bottom bar's RELOAD and HOME
 * act on the same session the panel shows: RELOAD retries failed checks, HOME
 * stops the running session safely before navigating away.
 */
@Composable
fun SessionScreen(
    videoId: String,
    onHome: () -> Unit,
    onOpenInYouTube: (String) -> Unit,
    diagnosticsViewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryActionButton(
                    text = "HOME",
                    icon = Icons.Filled.Home,
                    onClick = {
                        diagnosticsViewModel.stop()
                        onHome()
                    },
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "RELOAD",
                    icon = Icons.Filled.Refresh,
                    onClick = diagnosticsViewModel::reloadFailed,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PlayerScreen(
                videoId = videoId,
                onOpenInYouTube = onOpenInYouTube,
                modifier = Modifier.padding(top = 8.dp),
            )
            // Console of the ten endpoints on duty for this session (Bab 8).
            DiagnosticsPanel(viewModel = diagnosticsViewModel)
        }
    }
}
