package com.altomedia.smartghost.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.smartghost.coreui.component.PrimaryActionButton
import com.altomedia.smartghost.coreui.component.SecondaryActionButton
import com.altomedia.smartghost.coreui.component.SectionLabel
import com.altomedia.smartghost.coreui.component.SmartCard

/**
 * Home screen (Bab 4): logo, URL field, START and a status summary. SMARTGHOST
 * opens a single official YouTube player for the link the user enters.
 */
@Composable
fun HomeScreen(
    onStartSession: (StartSessionRequest) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startRequests.collect { onStartSession(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            UrlField(
                value = state.urlInput,
                validation = state.urlValidation,
                onValueChange = viewModel::onUrlChanged,
            )
            PrimaryActionButton(
                text = "START",
                onClick = viewModel::onStart,
                enabled = state.canStart,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            StatusSummary(state)
            ComplianceNote()
            Spacer(Modifier.height(4.dp))
            SecondaryActionButton(
                text = "Pengaturan",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.message != null) {
                Text(
                    text = state.message!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = "SMARTGHOST",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "ALTOMEDIA · Android",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun UrlField(
    value: String,
    validation: UrlValidation,
    onValueChange: (String) -> Unit,
) {
    Column {
        SectionLabel("TAUTAN YOUTUBE")
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://youtube.com/watch?v=...") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            isError = validation is UrlValidation.Invalid,
            shape = MaterialTheme.shapes.small,
        )
        if (validation is UrlValidation.Invalid) {
            Text(
                text = validation.message,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (validation is UrlValidation.Valid) {
            Text(
                text = "Tautan valid.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun StatusSummary(state: HomeUiState) {
    SmartCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Ringkasan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    !state.canStart -> "Masukkan tautan YouTube yang valid untuk memulai."
                    else -> "Siap memulai sesi pemutaran resmi."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComplianceNote() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(
            text = "SMARTGHOST membuka satu pemutar resmi YouTube yang Anda " +
                "kendalikan. Tidak ada penambah penonton: pemutaran sepenuhnya " +
                "mengikuti aturan YouTube.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
