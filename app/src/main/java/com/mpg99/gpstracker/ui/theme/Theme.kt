package com.mpg99.gpstracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Single accent: an emerald green, used for primary actions and route lines. */
val AccentGreen = Color(0xFF2FBF71)

private val Ink = Color(0xFF0D120F)
private val Snow = Color(0xFFF2F5EE)
private val Mist = Color(0xFF9BA89F)

// Dark palette: near-black green ground, two surface steps, one emerald accent.
private val DarkColors = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Ink,
    primaryContainer = Color(0xFF1D2620),
    onPrimaryContainer = Snow,

    secondary = Mist,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF1D2620),
    onSecondaryContainer = Snow,

    tertiary = AccentGreen,
    onTertiary = Ink,

    background = Ink,
    onBackground = Snow,
    surface = Ink,
    onSurface = Snow,
    surfaceVariant = Color(0xFF1D2620),
    onSurfaceVariant = Mist,
    surfaceContainerLow = Color(0xFF151C17),
    surfaceContainer = Color(0xFF151C17),
    surfaceContainerHigh = Color(0xFF1D2620),
    surfaceContainerHighest = Color(0xFF1D2620),

    outline = Color(0xFF2A3630),
    outlineVariant = Color(0xFF222D26),

    error = Color(0xFFFF9C82),
    onError = Ink,
)

/** Background color for the summary/track cards shown across the app. */
val SummaryCardColor = Color(0xFF151C17)

/** Flat background behind a route shape or map placeholder (covers without an image). */
val RouteBackdropColor = Color(0xFF101712)

/** Coral used for the "Detener" (stop recording) button and the active record tab. */
val StopButtonColor = Color(0xFFFF7A59)

/** Content color on top of [StopButtonColor]. */
val OnStopButtonColor = Color(0xFF1A0E0A)

/** Soft red for destructive actions on dark surfaces. */
val DangerColor = Color(0xFFFF9C82)

@Composable
fun GPSTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
