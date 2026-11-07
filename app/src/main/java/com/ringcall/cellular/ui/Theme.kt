package com.ringcall.cellular.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ringcall.cellular.domain.RingMode

private val Light = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF001257),
    secondaryContainer = Color(0xFFE2E1EC),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF2A3A9A),
    onPrimaryContainer = Color(0xFFDDE1FF),
)

/** Mode accent colors (container, content). */
fun modeColors(mode: RingMode, dark: Boolean): Pair<Color, Color> = when (mode) {
    RingMode.NORMAL -> if (dark) Color(0xFF1E4D2B) to Color(0xFFB7F1BF) else Color(0xFFD4F5D9) to Color(0xFF0B5D1E)
    RingMode.VIBRATE -> if (dark) Color(0xFF5A4300) to Color(0xFFFFE08A) else Color(0xFFFFF0C2) to Color(0xFF6B4E00)
    RingMode.SILENT -> if (dark) Color(0xFF5C1A1A) to Color(0xFFFFB4AB) else Color(0xFFFFDAD6) to Color(0xFF8C1D18)
}

/** Hebrew UI: always right-to-left, regardless of the system language. */
@Composable
fun RingTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
