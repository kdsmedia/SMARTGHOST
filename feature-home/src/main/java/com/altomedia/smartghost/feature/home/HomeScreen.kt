package com.altomedia.smartghost.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.smartghost.coreui.component.PrimaryActionButton
import com.altomedia.smartghost.coreui.component.SecondaryActionButton
import com.altomedia.smartghost.coreui.component.SectionLabel
import com.altomedia.smartghost.coreui.component.SmartCard
import java.io.InputStreamReader

/**
 * Home screen (Bab 4): logo, URL field, optional proxy.txt picker, START and a
 * status summary. The proxy file is only for diagnostics against the user's own
 * endpoints — never for disguising viewers.
 */
@Composable
fun HomeScreen(
    onStartSession: (StartSessionRequest) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val proxyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "proxy.txt"
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    InputStreamReader(stream).readText()
                }
            }.getOrNull()
            viewModel.onProxyFileLoaded(name, content)
        }
    }

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
            ProxySection(
                state = state,
                onPickFile = { proxyPicker.launch(arrayOf("text/plain", "text/*", "*/*")) },
                onClear = viewModel::clearProxy,
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
private fun ProxySection(
    state: HomeUiState,
    onPickFile: () -> Unit,
    onClear: () -> Unit,
) {
    Column {
        SectionLabel("PROXY DIAGNOSTIK (OPSIONAL)")
        SmartCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.proxyFileName ?: "Belum ada file dipilih",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (state.hasProxyLoaded) {
                            "${state.proxyEndpointCount} endpoint siap diuji"
                        } else {
                            "Untuk uji konektivitas server milik sendiri"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.hasProxyLoaded) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus file proxy")
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onPickFile) {
                    Icon(Icons.Filled.UploadFile, contentDescription = "Pilih file proxy")
                }
            }
        }
        if (state.proxyErrors.isNotEmpty()) {
            Text(
                text = "Baris bermasalah: " +
                    state.proxyErrors.take(3).joinToString(", ") { "baris ${it.line}" } +
                    if (state.proxyErrors.size > 3) ", …" else "",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
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
                "kendalikan. Panel 10 kartu adalah diagnostik jaringan, bukan " +
                "penambah penonton.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
