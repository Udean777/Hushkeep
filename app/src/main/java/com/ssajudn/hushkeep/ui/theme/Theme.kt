package com.ssajudn.hushkeep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HushkeepMossLight,
    onPrimary = HushkeepNight,
    primaryContainer = HushkeepNightMossContainer,
    onPrimaryContainer = HushkeepMossLight,
    secondary = HushkeepClayLight,
    onSecondary = HushkeepNight,
    secondaryContainer = HushkeepNightClayContainer,
    onSecondaryContainer = Color(0xFFFFD9CC),
    background = HushkeepNight,
    onBackground = HushkeepPaper,
    surface = HushkeepNightSurface,
    onSurface = HushkeepPaper,
    surfaceVariant = HushkeepNightElevated,
    onSurfaceVariant = HushkeepNightMuted,
    outline = HushkeepNightLine,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = HushkeepMoss,
    onPrimary = HushkeepSurfaceElevated,
    primaryContainer = Color(0xFFD3E2D3),
    onPrimaryContainer = Color(0xFF183A29),
    secondary = HushkeepClay,
    onSecondary = HushkeepSurfaceElevated,
    secondaryContainer = HushkeepClayContainer,
    onSecondaryContainer = Color(0xFF3A0B02),
    background = HushkeepPaper,
    onBackground = HushkeepInk,
    surface = HushkeepSurface,
    onSurface = HushkeepInk,
    surfaceVariant = Color(0xFFE7E4DB),
    onSurfaceVariant = HushkeepMutedInk,
    outline = HushkeepLine,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun HushkeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HushkeepShapes,
        content = content
    )
}
