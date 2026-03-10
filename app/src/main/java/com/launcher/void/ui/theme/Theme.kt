package com.launcher.void.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoidDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0D0D0D),
    onSurface = Color.White,
    outline = Color(0x33FFFFFF)
)

@Composable
fun VoidAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidDarkColorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
