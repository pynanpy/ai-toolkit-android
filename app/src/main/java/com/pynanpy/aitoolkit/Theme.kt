package com.pynanpy.aitoolkit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,

    secondary = Color(0xFF6366F1),
    onSecondary = Color.White,

    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF17181C),

    surface = Color.White,
    onSurface = Color(0xFF17181C),

    surfaceVariant = Color(0xFFE9EAF0),
    onSurfaceVariant = Color(0xFF3F4148)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1D1B20),

    secondary = Color(0xFFC4B5FD),
    onSecondary = Color(0xFF211F26),

    background = Color(0xFF101114),
    onBackground = Color(0xFFE6E1E6),

    surface = Color(0xFF17181C),
    onSurface = Color(0xFFE6E1E6),

    surfaceVariant = Color(0xFF292B31),
    onSurfaceVariant = Color(0xFFD0D1D8)
)

@Composable
fun AIToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colors =
        if (darkTheme) {
            DarkColors
        } else {
            LightColors
        }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}