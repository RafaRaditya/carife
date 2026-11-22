package com.carife.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CarifeColorScheme = lightColorScheme(
    primary = CarifePrimary,
    onPrimary = CarifeOnPrimary,
    primaryContainer = CarifePrimaryContainer,
    onPrimaryContainer = CarifeOnPrimaryContainer,
    secondary = CarifeSecondary,
    onSecondary = CarifeOnSecondary,
    secondaryContainer = CarifeSecondaryContainer,
    onSecondaryContainer = CarifeOnSecondaryContainer,
    tertiary = CarifeTertiary,
    onTertiary = CarifeOnPrimary,
    tertiaryContainer = CarifeTertiaryFixed,
    onTertiaryContainer = CarifeOnTertiaryFixed,
    error = CarifeError,
    onError = CarifeOnError,
    errorContainer = CarifeErrorContainer,
    onErrorContainer = CarifeOnErrorContainer,
    background = CarifeCanvas,
    onBackground = CarifeOnSurface,
    surface = CarifeCanvas,
    onSurface = CarifeOnSurface,
    surfaceVariant = CarifeSurfaceContainerHighest,
    onSurfaceVariant = CarifeOnSurfaceVariant,
    surfaceContainerLowest = CarifeSurfaceContainerLowest,
    surfaceContainerLow = CarifeSurfaceContainerLow,
    surfaceContainer = CarifeSurfaceContainer,
    surfaceContainerHigh = CarifeSurfaceContainerHigh,
    surfaceContainerHighest = CarifeSurfaceContainerHighest,
    outline = CarifeOutline,
    outlineVariant = CarifeOutlineVariant,
)

@Composable
fun CarifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CarifeColorScheme,
        typography = CarifeTypography,
        shapes = CarifeShapes,
        content = content,
    )
}
