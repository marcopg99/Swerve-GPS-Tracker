package com.mpg99.gpstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark palette built around several greens.
private val DarkGreenColors = darkColorScheme(
    primary = Color(0xFF7FD98B),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1F5B2A),
    onPrimaryContainer = Color(0xFF9BF6A6),

    secondary = Color(0xFF9CD3A0),
    onSecondary = Color(0xFF08381A),
    secondaryContainer = Color(0xFF34513B),
    onSecondaryContainer = Color(0xFFB8EFBC),

    tertiary = Color(0xFF6FD4B0),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF105040),
    onTertiaryContainer = Color(0xFF8CF0CC),

    background = Color(0xFF0E140F),
    onBackground = Color(0xFFDFE4DC),
    surface = Color(0xFF0E140F),
    onSurface = Color(0xFFDFE4DC),
    surfaceVariant = Color(0xFF3B4A3D),
    onSurfaceVariant = Color(0xFFBFCBBE),
    surfaceContainer = Color(0xFF19211A),
    surfaceContainerHigh = Color(0xFF232C24),

    outline = Color(0xFF8A9889),
    outlineVariant = Color(0xFF3B4A3D),
)

@Composable
fun GPSTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = DarkGreenColors, content = content)
}
