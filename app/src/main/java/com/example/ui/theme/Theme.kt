package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeobrutalistLightColorScheme = lightColorScheme(
    primary = NeoYellow,
    onPrimary = NeoDark,
    primaryContainer = NeoYellow,
    onPrimaryContainer = NeoDark,
    secondary = NeoMint,
    onSecondary = NeoDark,
    secondaryContainer = NeoMint,
    onSecondaryContainer = NeoDark,
    tertiary = NeoPink,
    onTertiary = NeoWhite,
    background = NeoBg,
    onBackground = NeoDark,
    surface = NeoSurface,
    onSurface = NeoDark,
    surfaceVariant = NeoSurfaceVariant,
    onSurfaceVariant = NeoDark,
    outline = NeoBorder,
    error = NeoRed,
    onError = NeoWhite
)

private val NeobrutalistDarkColorScheme = darkColorScheme(
    primary = NeoYellow,
    onPrimary = NeoDark,
    primaryContainer = NeoYellow,
    onPrimaryContainer = NeoDark,
    secondary = NeoMint,
    onSecondary = NeoDark,
    secondaryContainer = NeoMint,
    onSecondaryContainer = NeoDark,
    tertiary = NeoPink,
    onTertiary = NeoWhite,
    background = NeoDark,
    onBackground = NeoWhite,
    surface = NeoDark,
    onSurface = NeoWhite,
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = NeoWhite,
    outline = NeoWhite,
    error = NeoRed,
    onError = NeoWhite
)

@Composable
fun GalleryProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NeobrutalistDarkColorScheme else NeobrutalistLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
