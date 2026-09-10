package com.mori.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mori.core.model.ColorSchemeChoice

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
 * True-black override for dark mode: backgrounds go pure black for OLED power savings
 * while containers keep a whisper of tone so scrims and sheets stay legible.
 */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C0C0C),
    surfaceContainer = Color(0xFF131313),
    surfaceContainerHigh = Color(0xFF1B1B1B),
    surfaceContainerHighest = Color(0xFF232323),
)

private fun presetScheme(choice: ColorSchemeChoice, darkTheme: Boolean): ColorScheme {
    if (darkTheme) {
        return when (choice) {
            ColorSchemeChoice.MORI -> DarkColors
            ColorSchemeChoice.OCEAN -> darkColorScheme(
                primary = OceanColors.PrimaryDark,
                onPrimary = OceanColors.OnPrimaryDark,
                primaryContainer = OceanColors.ContainerDark,
                onPrimaryContainer = OceanColors.OnContainerDark,
                secondary = OceanColors.SecondaryDark,
                tertiary = OceanColors.TertiaryDark,
            )
            ColorSchemeChoice.FOREST -> darkColorScheme(
                primary = ForestColors.PrimaryDark,
                onPrimary = ForestColors.OnPrimaryDark,
                primaryContainer = ForestColors.ContainerDark,
                onPrimaryContainer = ForestColors.OnContainerDark,
                secondary = ForestColors.SecondaryDark,
                tertiary = ForestColors.TertiaryDark,
            )
            ColorSchemeChoice.SUNSET -> darkColorScheme(
                primary = SunsetColors.PrimaryDark,
                onPrimary = SunsetColors.OnPrimaryDark,
                primaryContainer = SunsetColors.ContainerDark,
                onPrimaryContainer = SunsetColors.OnContainerDark,
                secondary = SunsetColors.SecondaryDark,
                tertiary = SunsetColors.TertiaryDark,
            )
        }
    }
    return when (choice) {
        ColorSchemeChoice.MORI -> LightColors
        ColorSchemeChoice.OCEAN -> lightColorScheme(
            primary = OceanColors.PrimaryLight,
            onPrimary = OceanColors.OnPrimaryLight,
            primaryContainer = OceanColors.ContainerLight,
            onPrimaryContainer = OceanColors.OnContainerLight,
            secondary = OceanColors.SecondaryLight,
            tertiary = OceanColors.TertiaryLight,
        )
        ColorSchemeChoice.FOREST -> lightColorScheme(
            primary = ForestColors.PrimaryLight,
            onPrimary = ForestColors.OnPrimaryLight,
            primaryContainer = ForestColors.ContainerLight,
            onPrimaryContainer = ForestColors.OnContainerLight,
            secondary = ForestColors.SecondaryLight,
            tertiary = ForestColors.TertiaryLight,
        )
        ColorSchemeChoice.SUNSET -> lightColorScheme(
            primary = SunsetColors.PrimaryLight,
            onPrimary = SunsetColors.OnPrimaryLight,
            primaryContainer = SunsetColors.ContainerLight,
            onPrimaryContainer = SunsetColors.OnContainerLight,
            secondary = SunsetColors.SecondaryLight,
            tertiary = SunsetColors.TertiaryLight,
        )
    }
}

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
    colorScheme: ColorSchemeChoice = ColorSchemeChoice.MORI,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> presetScheme(colorScheme, darkTheme)
    }
    val scheme = if (amoled && darkTheme) baseScheme.amoled() else baseScheme

    MaterialTheme(
        colorScheme = scheme,
        typography = MoriTypography,
        shapes = MoriShapes,
        content = content,
    )
}
