package com.mori.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = MoriSeedDark,
    onPrimary = MoriOnSeedLight,
    primaryContainer = MoriSeedContainerLight,
    onPrimaryContainer = MoriOnSeedContainerLight,
    secondary = MoriSecondaryLight,
    tertiary = MoriTertiaryLight,
)

private val DarkColors = darkColorScheme(
    primary = MoriSeedLight,
    onPrimary = MoriOnSeedDark,
    primaryContainer = MoriSeedContainerDark,
    onPrimaryContainer = MoriOnSeedContainerDark,
    secondary = MoriSecondaryDark,
    tertiary = MoriTertiaryDark,
)

/**
 * Mori theme: Material 3 Expressive, dark-first gallery aesthetic with dynamic color.
 *
 * Surfaces use tonal roles (never raw colors); components consume only
 * `MaterialTheme.colorScheme` / `typography` / `shapes`.
 */
@Composable
fun MoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoriTypography,
        shapes = MoriShapes,
        content = content,
    )
}
