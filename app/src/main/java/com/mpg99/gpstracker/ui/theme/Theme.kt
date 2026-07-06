package com.mpg99.gpstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark palette built around several greens.
private val DarkGreenColors = darkColorScheme(
    primary = Color(0xFF31CB00),
    onPrimary = Color(0xFF0F1D0F),
    primaryContainer = Color(0xFF1E441E),
    onPrimaryContainer = Color(0xFFFFFFFF),

    secondary = Color(0xFF9CD3A0),
    onSecondary = Color(0xFF08381A),
    secondaryContainer = Color(0xFF34513B),
    onSecondaryContainer = Color(0xFFB8EFBC),

    tertiary = Color(0xFF6FD4B0),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF105040),
    onTertiaryContainer = Color(0xFF8CF0CC),

    background = Color(0xFF0F110F),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0F110F),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1D211D),
    onSurfaceVariant = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFff00ff),
    surfaceContainerHigh = Color(0xFF0F110F),

    outline = Color(0xFFffffff),
    outlineVariant = Color(0xFFDADADA),
)

/** Background color for the summary/track cards shown across the app. */
val SummaryCardColor = Color(0xFF1E441E)

/** Background color for the "Detener" (stop recording) button. */
val StopButtonColor = Color(0xFFae2012)

@Composable
fun GPSTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = DarkGreenColors, content = content)
}
