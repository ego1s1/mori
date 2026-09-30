@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mori.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Named display faces on top of the variable [R.font.roboto_flex] family.
 *
 * Mirrors the reference app's three-face scheme (top-bar title, secondary
 * titles, annotated-string pair) on Roboto Flex. Roboto Flex exposes weight
 * and width axes; the reference's proprietary rounded axis has no open
 * equivalent, so geometry that depended on it is approximated with weight and
 * width only.
 */
data class AppFonts(
    /** Heavy, wide face for top-bar titles. */
    val topBarTitle: FontFamily,
    /** Regular + semibold pair used inside annotated strings. */
    val annotatedString: FontFamily,
)

/** Roboto Flex, weight 900 at 112.5% width — the widest, heaviest display face. */
private val RobotoFlexTopBar = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.width(112.5f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(32.sp),
        ),
    ),
)

/** Regular + semibold pair for inline emphasis in annotated strings. */
private val RobotoFlexAnnotated = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(16.sp),
        ),
    ),
    Font(
        resId = R.font.roboto_flex,
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
internal fun appFonts(): AppFonts = AppFonts(
    topBarTitle = RobotoFlexTopBar,
    annotatedString = RobotoFlexAnnotated,
)

/** Display faces for the current theme; read by screen headers. */
val LocalAppFonts = staticCompositionLocalOf { appFonts() }
