package com.altomedia.smartghost.feature.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.smartghost.data.diagnostics.DiagnosticResult
import com.altomedia.smartghost.data.diagnostics.DiagnosticStatus
import kotlinx.coroutines.delay

/** Console palette — deliberately terminal-like, independent of the app theme. */
private val ConsoleBackground = Color(0xFF050805)
private val ConsoleGreen = Color(0xFF33FF77)
private val ConsoleGreenDim = Color(0xFF1E9E4A)
private val ConsoleAmber = Color(0xFFFFC53D)
private val ConsoleRed = Color(0xFFFF5D5D)
private val ConsoleBorder = Color(0xFF1E9E4A)

/**
 * Terminal-style console listing the ten endpoints on duty for this session.
 *
 * It renders monospace green-on-black and updates live as each check runs. This
 * is the honest counterpart of a "viewer list": every line is one endpoint the
 * device is probing for connectivity, never a person watching the video.
 */
@Composable
fun ProxyConsole(
    cards: List<DiagnosticResult>,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    // A one-second tick keeps the "running" lines and the elapsed clock moving
    // while a session is active, and stops as soon as it ends.
    val elapsedSeconds by produceState(initialValue = 0L, key1 = isRunning) {
        if (!isRunning) return@produceState
        var seconds = 0L
        while (true) {
            delay(1_000)
            seconds += 1
            value = seconds
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ConsoleBackground)
            .border(1.dp, ConsoleBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        ConsoleHeader(isRunning = isRunning, elapsedSeconds = elapsedSeconds, onDuty = cards.count {
            it.status != DiagnosticStatus.NOT_TESTED
        })

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            cards.forEach { card -> ConsoleLine(card = card, isRunning = isRunning) }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "> _",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = ConsoleGreen,
        )
    }
}

@Composable
private fun ConsoleHeader(isRunning: Boolean, elapsedSeconds: Long, onDuty: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isRunning) "● LIVE" else "○ IDLE",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) ConsoleGreen else ConsoleGreenDim,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$onDuty/10 proxy bertugas",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = ConsoleGreenDim,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatElapsed(elapsedSeconds),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = if (isRunning) ConsoleGreen else ConsoleGreenDim,
        )
    }
}

@Composable
private fun ConsoleLine(card: DiagnosticResult, isRunning: Boolean) {
    val (marker, markerColor) = when (card.status) {
        DiagnosticStatus.RUNNING -> "~" to ConsoleAmber
        DiagnosticStatus.SUCCESS -> "✓" to ConsoleGreen
        DiagnosticStatus.FAILURE -> "✗" to ConsoleRed
        DiagnosticStatus.NOT_TESTED -> "·" to ConsoleGreenDim
    }
    val status = when (card.status) {
        DiagnosticStatus.RUNNING -> if (isRunning) "menonton…" else "berjalan…"
        DiagnosticStatus.SUCCESS -> "ok ${card.httpCode ?: 200} · ${card.latencyMs ?: 0}ms"
        DiagnosticStatus.FAILURE -> "gagal · ${card.errorSummary ?: "error"}"
        DiagnosticStatus.NOT_TESTED -> "menunggu"
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "%02d".format(card.index),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = ConsoleGreenDim,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = card.endpointLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = ConsoleGreen,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$marker $status",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = markerColor,
        )
    }
}

/** mm:ss clock for the console header. */
internal fun formatElapsed(seconds: Long): String {
    val safe = if (seconds < 0) 0 else seconds
    return "%02d:%02d".format(safe / 60, safe % 60)
}
