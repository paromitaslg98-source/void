package com.knownassurajit.app.launcher.voidlauncher.ui.theme

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

// MD3 Expressive — 3-tier shape vocabulary
private val VoidExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),     // flat list rows
    small = RoundedCornerShape(8.dp),          // chips, small pills
    medium = RoundedCornerShape(8.dp),         // interactive elements
    large = RoundedCornerShape(20.dp),         // cards, sheets
    extraLarge = RoundedCornerShape(20.dp)     // search bar, bottom sheets
)

@Composable
fun VoidAppTheme(appFont: String = "inter", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoidDarkColorScheme,
        typography = getTypography(appFont),
        shapes = VoidExpressiveShapes,
        content = content
    )
}
