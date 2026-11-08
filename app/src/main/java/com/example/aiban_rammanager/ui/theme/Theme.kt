package com.example.aiban_rammanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = BackgroundDark,

    secondary = GreenSecondary,
    onSecondary = BackgroundDark,

    primaryContainer = GreenContainer,
    onPrimaryContainer = TextPrimary,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,

    outline = BorderDark,
    error = ErrorRed
)

@Composable
fun Aiban_RamManagerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}