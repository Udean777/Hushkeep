package com.ssajudn.hushkeep.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = HushkeepCoralLight,
    onPrimary = HushkeepNight,
    primaryContainer = HushkeepNightMossContainer,
    onPrimaryContainer = HushkeepCoralLight,
    secondary = HushkeepSunLight,
    onSecondary = HushkeepNight,
    secondaryContainer = HushkeepNightClayContainer,
    onSecondaryContainer = HushkeepSunLight,
    tertiary = HushkeepMint,
    onTertiary = HushkeepNight,
    tertiaryContainer = HushkeepNightMintContainer,
    onTertiaryContainer = HushkeepMint,
    background = HushkeepNight,
    onBackground = HushkeepPaper,
    surface = HushkeepNightSurface,
    onSurface = HushkeepPaper,
    surfaceVariant = HushkeepNightElevated,
    onSurfaceVariant = HushkeepNightMuted,
    outline = HushkeepNightLine,
    outlineVariant = HushkeepNightLine,
    error = HushkeepCoralLight,
    onError = HushkeepNight,
    errorContainer = HushkeepNightMossContainer,
    onErrorContainer = HushkeepCoralLight,
)

private val LightColorScheme = lightColorScheme(
    primary = HushkeepCoralPressed,
    onPrimary = HushkeepPaper,
    primaryContainer = HushkeepCoralContainer,
    onPrimaryContainer = HushkeepInk,
    secondary = HushkeepSun,
    onSecondary = HushkeepInk,
    secondaryContainer = HushkeepSunContainer,
    onSecondaryContainer = HushkeepInk,
    tertiary = HushkeepMint,
    onTertiary = HushkeepInk,
    tertiaryContainer = HushkeepMintContainer,
    onTertiaryContainer = HushkeepInk,
    background = HushkeepPaper,
    onBackground = HushkeepInk,
    surface = HushkeepSurface,
    onSurface = HushkeepInk,
    surfaceVariant = HushkeepSkyContainer,
    onSurfaceVariant = HushkeepMutedInk,
    outline = HushkeepLine,
    outlineVariant = HushkeepLine,
    error = HushkeepCoralPressed,
    onError = HushkeepSurfaceElevated,
    errorContainer = HushkeepCoralContainer,
    onErrorContainer = HushkeepInk,
)

@Composable
fun HushkeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)

            // ThemeMode can override the device theme, so do not rely only on
            // ComponentActivity.enableEdgeToEdge()'s system-mode detection.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HushkeepShapes,
        content = content
    )
}
