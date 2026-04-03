package com.launcher.projectvoid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val VoidDarkColorScheme = darkColorScheme(
    primary = VoidPrimary,
    onPrimary = VoidOnPrimary,
    primaryContainer = VoidPrimaryContainer,
    onPrimaryContainer = VoidOnPrimaryContainer,
    secondary = VoidSecondary,
    onSecondary = VoidOnSecondary,
    secondaryContainer = VoidSecondaryContainer,
    onSecondaryContainer = VoidOnSecondaryContainer,
    tertiary = VoidTertiary,
    onTertiary = VoidOnTertiary,
    tertiaryContainer = VoidTertiaryContainer,
    onTertiaryContainer = VoidOnTertiaryContainer,
    error = VoidError,
    onError = VoidOnError,
    errorContainer = VoidErrorContainer,
    onErrorContainer = VoidOnErrorContainer,
    background = VoidBlack,
    onBackground = VoidOnSurface,
    surface = VoidSurface,
    onSurface = VoidOnSurface,
    surfaceVariant = VoidSurfaceContainer,
    onSurfaceVariant = VoidOnSurfaceVariant,
    outline = VoidOutline,
    outlineVariant = VoidOutlineVariant,
    inverseSurface = VoidInverseSurface,
    inverseOnSurface = VoidInverseOnSurface,
    inversePrimary = VoidInversePrimary,
    scrim = VoidScrim,
    surfaceContainerHighest = VoidSurfaceContainerHighest,
    surfaceContainerHigh = VoidSurfaceContainerHigh,
    surfaceContainer = VoidSurfaceContainer,
    surfaceContainerLow = VoidSurfaceContainerLow,
    surfaceBright = VoidSurfaceContainerHighest,
    surfaceDim = VoidBlack
)

private val VoidExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VoidAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidDarkColorScheme,
        typography = VoidTypography,
        shapes = VoidExpressiveShapes,
        content = content
    )
}
