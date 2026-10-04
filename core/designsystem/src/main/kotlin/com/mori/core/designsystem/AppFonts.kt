@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mori.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Named display faces on top of the variable [R.font.google_sans_flex] family.
 *
 * Google Sans Flex exposes weight, width (25–151), grade, optical size,
 * roundness (ROND) and slant axes. Per minimalist guidance, normal 100% width
 * is maintained across the application, with wide 125% width reserved strictly
 * for hero numerals in reading stats, accompanied by staggered intermediate
 * widths for minutes and units.
 */
data class AppFonts(
    /** Standard-width heavy face for top-bar titles. */
    val topBarTitle: FontFamily,
    /** Regular + semibold pair used inside annotated strings. */
    val annotatedString: FontFamily,
    /** Wide, heavy display face reserved strictly for stats hero numerals. */
    val displayFlex: FontFamily,
    /** Soft display face: normal width + full roundness, for hero moments. */
    val displaySoft: FontFamily,
    /** Staggered intermediate face for secondary numerals (e.g. minutes in reading stats). */
    val displayFlexMedium: FontFamily = displayFlex,
    /** Staggered compact unit face (e.g. 'h' and 'm' unit badges in stats). */
    val displayUnit: FontFamily = topBarTitle,
)

/** Google Sans Flex, weight 900 at standard 100% width for clean, minimal top bars. */
private val GoogleSansFlexTopBar = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(32.sp),
        ),
    ),
)

/** Google Sans Flex, weight 900 at wide 125% width — reserved strictly for stats page hero numerals. */
private fun flexDisplay(grade: Int) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(125f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(48.sp),
        ),
    ),
    // True slanted file variation: TextStyle(Italic) resolves here instead
    // of synthesizing an oblique.
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(125f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(48.sp),
            FontVariation.slant(-10f),
        ),
    ),
)

/** Google Sans Flex, weight 800 at width 115% for staggered minutes numerals. */
private fun flexMediumDisplay(grade: Int) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(115f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(38.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.ExtraBold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(115f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(38.sp),
            FontVariation.slant(-8f),
        ),
    ),
)

/** Google Sans Flex, weight 700 at width 95% with slight slant for staggered duration units ('h', 'm', 's'). */
private fun flexUnit(grade: Int) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(95f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(18.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(95f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(18.sp),
            FontVariation.slant(-4f),
        ),
    ),
)

/** Google Sans Flex soft display: standard 100% width, full ROND roundness. */
private fun softDisplay(grade: Int) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(100f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(48.sp),
            FontVariation.Setting("ROND", 100f),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(100f),
            FontVariation.grade(grade),
            FontVariation.opticalSizing(48.sp),
            FontVariation.Setting("ROND", 100f),
            FontVariation.slant(-10f),
        ),
    ),
)

/** Regular + semibold pair for inline emphasis in annotated strings. */
private val GoogleSansFlexAnnotated = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(16.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(16.sp),
        ),
    ),
)

/** Builds the app's display faces; provided once by [MoriTheme]. */
internal fun appFonts(darkTheme: Boolean): AppFonts {
    // Grade lifts hero strokes in dark themes only: same layout, more ink.
    val grade = if (darkTheme) HERO_DARK_GRADE else 0
    return AppFonts(
        topBarTitle = GoogleSansFlexTopBar,
        annotatedString = GoogleSansFlexAnnotated,
        displayFlex = flexDisplay(grade),
        displayFlexMedium = flexMediumDisplay(grade),
        displayUnit = flexUnit(grade),
        displaySoft = softDisplay(grade),
    )
}

/** Grade boost for hero faces in dark themes. */
private const val HERO_DARK_GRADE = 25

/** Display faces for the current theme; read by screen headers. */
val LocalAppFonts = staticCompositionLocalOf { appFonts(darkTheme = false) }
