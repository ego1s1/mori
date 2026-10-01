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
 * roundness (ROND) and slant axes; the faces below pin them per role.
 * Italics resolve to the true slanted file variation (slnt −10), never a
 * synthetic oblique. Roundness is reserved for hero faces; chrome stays
 * sharp. Grade lifts heroes in dark themes only.
 */
data class AppFonts(
    /** Heavy, wide face for top-bar titles. */
    val topBarTitle: FontFamily,
    /** Regular + semibold pair used inside annotated strings. */
    val annotatedString: FontFamily,
    /** Wide, heavy display face for hero numerals. */
    val displayFlex: FontFamily,
    /** Soft display face: max width + full roundness, for hero moments. */
    val displaySoft: FontFamily,
)

/** Google Sans Flex, weight 900 at 112.5% width — the widest, heaviest display face. */
private val GoogleSansFlexTopBar = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(112.5f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(32.sp),
        ),
    ),
)

/** Google Sans Flex, weight 900 at 125% width — hero numerals at display sizes. */
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

/** Google Sans Flex soft display: wide 135% width, full ROND roundness. */
private fun softDisplay(grade: Int) = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(135f),
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
            FontVariation.width(135f),
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
        displaySoft = softDisplay(grade),
    )
}

/** Grade boost for hero faces in dark themes. */
private const val HERO_DARK_GRADE = 25

/** Display faces for the current theme; read by screen headers. */
val LocalAppFonts = staticCompositionLocalOf { appFonts(darkTheme = false) }
