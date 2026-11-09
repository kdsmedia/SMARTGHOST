package com.altomedia.smartghost.coreui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = NavyElevated,
    onPrimaryContainer = CyanBright,
    secondary = CyanAccent,
    onSecondary = NavyDeep,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSoft,
    error = StatusFailure,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = NavyDeep,
    secondary = CyanAccent,
    onSecondary = NavyDeep,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = StatusFailure,
    onError = Color.White,
)

/** Semantic colors for diagnostic status — not part of Material color roles. */
data class StatusColors(
    val neutral: Color,
    val running: Color,
    val success: Color,
    val failure: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        neutral = StatusNeutral,
        running = StatusRunning,
        success = StatusSuccess,
        failure = StatusFailure,
    )
}

/**
 * SMARTGHOST theme. Dark navy/charcoal by default; light theme available (Bab 4).
 */
@Composable
fun SmartGhostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartGhostTypography,
        shapes = SmartGhostShapes,
        content = content,
    )
}
