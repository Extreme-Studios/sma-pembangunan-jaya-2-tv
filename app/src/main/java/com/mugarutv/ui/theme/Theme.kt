package com.mugarutv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NuLightColorScheme = lightColorScheme(
    primary = NuGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = NuGreenLight,
    onPrimaryContainer = NuGreenDark,
    secondary = NuGoldAccent,
    onSecondary = Color.Black,
    background = BackgroundWhite,
    onBackground = Color(0xFF1A1C1E),
    surface = SurfaceWhite,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF2F4F7),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    error = ErrorColor
)

private val NuDarkColorScheme = lightColorScheme(
    primary = NuGreenPrimary,
    onPrimary = Color.White,
    background = BackgroundWhite,
    surface = SurfaceWhite,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

@Composable
fun MugaruTvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NuDarkColorScheme else NuLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val MaterialTheme.success: Color
    @Composable
    get() = SuccessColor
