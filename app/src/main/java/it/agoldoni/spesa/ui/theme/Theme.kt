package it.agoldoni.spesa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpesaLightColorScheme = lightColorScheme(
    primary = SpesaGreen,
    onPrimary = Color.White,
    primaryContainer = SpesaGreenLight,
    onPrimaryContainer = SpesaGreenDark,
    surface = SpesaSurface,
    onSurface = SpesaOnSurface,
    background = SpesaSurface,
    onBackground = SpesaOnSurface,
    outline = SpesaOutline,
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF555555)
)

private val SpesaDarkColorScheme = darkColorScheme(
    primary = SpesaGreen,
    onPrimary = Color.White,
    primaryContainer = SpesaGreenDark,
    onPrimaryContainer = SpesaGreenLight,
    surface = SpesaSurfaceDark,
    onSurface = SpesaOnSurfaceDark,
    background = SpesaSurfaceDark,
    onBackground = SpesaOnSurfaceDark,
    outline = SpesaOutlineDark,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

@Composable
fun SpesaTheme(isDark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark) SpesaDarkColorScheme else SpesaLightColorScheme,
        typography = SpesaTypography,
        content = content
    )
}
