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
    secondaryContainer = containerFor(MoriSecondaryLight, darkTheme = false).container,
    onSecondaryContainer = containerFor(MoriSecondaryLight, darkTheme = false).onContainer,
    tertiary = MoriTertiaryLight,
    tertiaryContainer = containerFor(MoriTertiaryLight, darkTheme = false).container,
    onTertiaryContainer = containerFor(MoriTertiaryLight, darkTheme = false).onContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = MoriSeedLight,
    onPrimary = MoriOnSeedDark,
    primaryContainer = MoriSeedContainerDark,
    onPrimaryContainer = MoriOnSeedContainerDark,
    secondary = MoriSecondaryDark,
    secondaryContainer = containerFor(MoriSecondaryDark, darkTheme = true).container,
    onSecondaryContainer = containerFor(MoriSecondaryDark, darkTheme = true).onContainer,
    tertiary = MoriTertiaryDark,
    tertiaryContainer = containerFor(MoriTertiaryDark, darkTheme = true).container,
    onTertiaryContainer = containerFor(MoriTertiaryDark, darkTheme = true).onContainer,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceContainerLowest = darkSurfaceRamp(MoriSeedLight).lowest,
    surfaceContainerLow = darkSurfaceRamp(MoriSeedLight).low,
    surfaceContainer = darkSurfaceRamp(MoriSeedLight).container,
    surfaceContainerHigh = darkSurfaceRamp(MoriSeedLight).high,
    surfaceContainerHighest = darkSurfaceRamp(MoriSeedLight).highest,
)

/**
 * True-black override for dark mode: backgrounds go pure black for OLED power
 * savings while containers deepen toward black instead of flattening to gray,
 * so the scheme's tint and the full on* hierarchy survive.
 */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLow.darkened(0.45f),
    surfaceContainer = surfaceContainer.darkened(0.35f),
    surfaceContainerHigh = surfaceContainerHigh.darkened(0.25f),
    surfaceContainerHighest = surfaceContainerHighest.darkened(0.12f),
    surfaceVariant = surfaceVariant.darkened(0.35f),
)

/** Full dark preset from accents: containers + error ramp derived, never baseline. */
private fun presetDarkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme {
    val ramp = darkSurfaceRamp(primary)
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = containerFor(secondary, darkTheme = true).container,
        onSecondaryContainer = containerFor(secondary, darkTheme = true).onContainer,
        tertiary = tertiary,
        tertiaryContainer = containerFor(tertiary, darkTheme = true).container,
        onTertiaryContainer = containerFor(tertiary, darkTheme = true).onContainer,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        surfaceContainerLowest = ramp.lowest,
        surfaceContainerLow = ramp.low,
        surfaceContainer = ramp.container,
        surfaceContainerHigh = ramp.high,
        surfaceContainerHighest = ramp.highest,
    )
}

/** Full light preset from accents: containers + error ramp derived, never baseline. */
private fun presetLightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    secondaryContainer = containerFor(secondary, darkTheme = false).container,
    onSecondaryContainer = containerFor(secondary, darkTheme = false).onContainer,
    tertiary = tertiary,
    tertiaryContainer = containerFor(tertiary, darkTheme = false).container,
    onTertiaryContainer = containerFor(tertiary, darkTheme = false).onContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private fun presetScheme(choice: ColorSchemeChoice, darkTheme: Boolean): ColorScheme {
    if (darkTheme) {
        return when (choice) {
            ColorSchemeChoice.MORI -> DarkColors
            ColorSchemeChoice.OCEAN -> presetDarkScheme(
                primary = OceanColors.PrimaryDark,
                onPrimary = OceanColors.OnPrimaryDark,
                primaryContainer = OceanColors.ContainerDark,
                onPrimaryContainer = OceanColors.OnContainerDark,
                secondary = OceanColors.SecondaryDark,
                tertiary = OceanColors.TertiaryDark,
            )
            ColorSchemeChoice.FOREST -> presetDarkScheme(
                primary = ForestColors.PrimaryDark,
                onPrimary = ForestColors.OnPrimaryDark,
                primaryContainer = ForestColors.ContainerDark,
                onPrimaryContainer = ForestColors.OnContainerDark,
                secondary = ForestColors.SecondaryDark,
                tertiary = ForestColors.TertiaryDark,
            )
            ColorSchemeChoice.SUNSET -> presetDarkScheme(
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
        ColorSchemeChoice.OCEAN -> presetLightScheme(
            primary = OceanColors.PrimaryLight,
            onPrimary = OceanColors.OnPrimaryLight,
            primaryContainer = OceanColors.ContainerLight,
            onPrimaryContainer = OceanColors.OnContainerLight,
            secondary = OceanColors.SecondaryLight,
            tertiary = OceanColors.TertiaryLight,
        )
        ColorSchemeChoice.FOREST -> presetLightScheme(
            primary = ForestColors.PrimaryLight,
            onPrimary = ForestColors.OnPrimaryLight,
            primaryContainer = ForestColors.ContainerLight,
            onPrimaryContainer = ForestColors.OnContainerLight,
            secondary = ForestColors.SecondaryLight,
            tertiary = ForestColors.TertiaryLight,
        )
        ColorSchemeChoice.SUNSET -> presetLightScheme(
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
