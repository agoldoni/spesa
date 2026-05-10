package it.agoldoni.spesa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SpesaColorScheme = lightColorScheme(
    primary = SpesaGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = SpesaGreenLight,
    onPrimaryContainer = SpesaGreenDark,
    surface = SpesaSurface,
    onSurface = SpesaOnSurface,
    background = SpesaSurface,
    onBackground = SpesaOnSurface,
    outline = SpesaOutline,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F1F1),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF555555)
)

@Composable
fun SpesaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpesaColorScheme,
        typography = SpesaTypography,
        content = content
    )
}
