package com.mori.core.designsystem

import androidx.compose.ui.graphics.Color
import com.mori.core.model.ColorSchemeChoice

// Static fallback palette (used when dynamic color is off or unavailable on API < 31).
// Seeds chosen for a deep-ink gallery feel; components must still use colorScheme roles.

internal val MoriSeedLight = Color(0xFFD6C2FF)
internal val MoriOnSeedDark = Color(0xFF2E1B5E)
internal val MoriSeedContainerDark = Color(0xFF44307E)
internal val MoriOnSeedContainerDark = Color(0xFFE8DEFF)
internal val MoriSecondaryDark = Color(0xFFC9C0D9)
internal val MoriTertiaryDark = Color(0xFFEFB8C8)

internal val MoriSeedDark = Color(0xFF6445A8)
internal val MoriOnSeedLight = Color(0xFFFFFFFF)
internal val MoriSeedContainerLight = Color(0xFFE6D9FF)
internal val MoriOnSeedContainerLight = Color(0xFF1E1042)
internal val MoriSecondaryLight = Color(0xFF585B93)
internal val MoriTertiaryLight = Color(0xFF8F4958)

// Preset schemes (primary / secondary / tertiary accents, light + dark).
// Hand-tuned M3-style tonal sets for when dynamic (wallpaper) color is off;
// Theme.kt derives every container/error/surface role from these accents.
internal object OceanColors {
    val PrimaryLight = Color(0xFF00696B)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFF9CF0F0)
    val OnContainerLight = Color(0xFF002020)
    val SecondaryLight = Color(0xFF4A6363)
    val TertiaryLight = Color(0xFF4D5F7C)
    val PrimaryDark = Color(0xFF80D5D6)
    val OnPrimaryDark = Color(0xFF003737)
    val ContainerDark = Color(0xFF004F50)
    val OnContainerDark = Color(0xFF9CF0F0)
    val SecondaryDark = Color(0xFFB0CCCB)
    val TertiaryDark = Color(0xFFB9C6E4)
}

internal object ForestColors {
    val PrimaryLight = Color(0xFF406836)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFC0F0BE)
    val OnContainerLight = Color(0xFF0A2007)
    val SecondaryLight = Color(0xFF54634D)
    val TertiaryLight = Color(0xFF6B6B22)
    val PrimaryDark = Color(0xFFA4D39A)
    val OnPrimaryDark = Color(0xFF12370F)
    val ContainerDark = Color(0xFF294E22)
    val OnContainerDark = Color(0xFFC0F0BE)
    val SecondaryDark = Color(0xFFB9CCB2)
    val TertiaryDark = Color(0xFFD4C489)
}

internal object SunsetColors {
    val PrimaryLight = Color(0xFF8C4E00)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFFFDDBA)
    val OnContainerLight = Color(0xFF2E1500)
    val SecondaryLight = Color(0xFF6F5B40)
    val TertiaryLight = Color(0xFF7E525E)
    val PrimaryDark = Color(0xFFFFB870)
    val OnPrimaryDark = Color(0xFF4A2800)
    val ContainerDark = Color(0xFF6B3D00)
    val OnContainerDark = Color(0xFFFFDDBA)
    val SecondaryDark = Color(0xFFD8C5A0)
    val TertiaryDark = Color(0xFFEFB8C8)
}

/** Swatch color identifying a scheme choice in pickers (light primary). */
fun ColorSchemeChoice.previewColor(): Color = when (this) {
    ColorSchemeChoice.MORI -> MoriSeedDark
    ColorSchemeChoice.OCEAN -> OceanColors.PrimaryLight
    ColorSchemeChoice.FOREST -> ForestColors.PrimaryLight
    ColorSchemeChoice.SUNSET -> SunsetColors.PrimaryLight
}

// Tonal derivation for scheme roles M3 baseline would otherwise supply.
// Preset schemes only hand-tune accents; every container/error role below is
// derived from those accents so non-dynamic Mori/Ocean/Forest/Sunset never
// render baseline-purple containers.

/** M3 baseline error ramp (scheme-independent red). */
internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

/** M3 baseline dark surface ramp (neutral violet-gray) containers whisper into. */
private val DarkLowest = Color(0xFF0F0D13)
private val DarkLow = Color(0xFF1D1B20)
private val DarkContainer = Color(0xFF211F26)
private val DarkHigh = Color(0xFF2B2930)
private val DarkHighest = Color(0xFF36343B)

/** Source-over blend of [foreground] onto this color. */
internal fun Color.blend(foreground: Color, alpha: Float): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - a) + foreground.red * a,
        green = green * (1f - a) + foreground.green * a,
        blue = blue * (1f - a) + foreground.blue * a,
        alpha = 1f,
    )
}

/** Darkens toward black by [fraction] (AMOLED depth without hue loss). */
internal fun Color.darkened(fraction: Float): Color {
    val f = (1f - fraction.coerceIn(0f, 1f))
    return copy(red = red * f, green = green * f, blue = blue * f)
}

/** Returns this hue/saturation at absolute [lightness] (M3-style tone targeting). */
internal fun Color.atLightness(lightness: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val l = lightness.coerceIn(0f, 1f)
    if (max == min) return Color(l, l, l, alpha)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        red -> ((green - blue) / d + (if (green < blue) 6f else 0f)) / 6f
        green -> ((blue - red) / d + 2f) / 6f
        else -> ((red - green) / d + 4f) / 6f
    }
    fun hue2rgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(hue2rgb(p, q, h + 1f / 3f), hue2rgb(p, q, h), hue2rgb(p, q, h - 1f / 3f), alpha)
}

/** Container role pair (container + on-container) derived from an accent. */
internal data class ContainerPair(val container: Color, val onContainer: Color)

/**
 * Tonal container derived from [accent]: light schemes land near tone 92
 * (dark on-container), dark schemes near tone 30 (light on-container).
 */
internal fun containerFor(accent: Color, darkTheme: Boolean): ContainerPair =
    if (darkTheme) {
        ContainerPair(
            container = accent.atLightness(0.30f),
            onContainer = accent.atLightness(0.90f),
        )
    } else {
        ContainerPair(
            container = accent.atLightness(0.92f),
            onContainer = accent.atLightness(0.12f),
        )
    }

/** Dark surface ramp carrying a whisper of [seed] over the neutral baseline. */
internal data class SurfaceRamp(
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
)

internal fun darkSurfaceRamp(seed: Color, whisper: Float = 0.08f): SurfaceRamp = SurfaceRamp(
    lowest = DarkLowest.blend(seed, whisper),
    low = DarkLow.blend(seed, whisper),
    container = DarkContainer.blend(seed, whisper),
    high = DarkHigh.blend(seed, whisper),
    highest = DarkHighest.blend(seed, whisper),
)
