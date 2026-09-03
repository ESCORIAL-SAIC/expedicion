package com.expedicion.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ExpBlue,
    onPrimary = Color.White,
    primaryContainer = ExpBlueLight,
    onPrimaryContainer = ExpBlueDark,
    secondary = ExpGreen,
    onSecondary = Color.White,
    secondaryContainer = ExpBlueLight,
    onSecondaryContainer = ExpBlueDark,
    surfaceVariant = ExpSurfaceVar,
    error = ExpRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00325B),
    primaryContainer = ExpBlueDark,
    onPrimaryContainer = ExpBlueLight,
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00352F),
    error = Color(0xFFEF9A9A),
)

@Composable
fun ExpedicionTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
