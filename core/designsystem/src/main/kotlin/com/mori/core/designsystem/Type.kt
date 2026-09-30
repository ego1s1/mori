@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.mori.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Mori type scale, built on the variable Roboto Flex family.
 *
 * Mirrors the reference app's split: display/headline/label roles and
 * `bodyLarge` ride the semibold face, while `titleLarge` and the small body
 * roles ride the regular face — hierarchy comes from weight contrast rather
 * than bespoke sizes. Sizes and line heights keep the Material 3 baseline;
 * only the family and weights are overridden, exactly as the reference does.
 */
internal val BodyFlex = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(14.sp),
        ),
    ),
)

internal val HeadingFlex = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(18.sp),
        ),
    ),
    // Bold entry: without it, bold emphases synthesize from the 600 face.
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(20.sp),
        ),
    ),
)

private val baseline = Typography()

private fun TextStyle.body() = copy(fontFamily = BodyFlex)
private fun TextStyle.heading() = copy(fontFamily = HeadingFlex)

/** The app type scale: the reference app's per-role weight split. */
val MoriTypography = baseline.copy(
    displayLarge = baseline.displayLarge.heading(),
    displayMedium = baseline.displayMedium.heading(),
    displaySmall = baseline.displaySmall.heading(),
    headlineLarge = baseline.headlineLarge.heading(),
    headlineMedium = baseline.headlineMedium.heading(),
    headlineSmall = baseline.headlineSmall.heading(),
    titleLarge = baseline.titleLarge.body(),
    titleMedium = baseline.titleMedium.heading(),
    titleSmall = baseline.titleSmall.heading(),
    bodyLarge = baseline.bodyLarge.heading(),
    bodyMedium = baseline.bodyMedium.body(),
    bodySmall = baseline.bodySmall.body(),
    labelLarge = baseline.labelLarge.heading(),
    labelMedium = baseline.labelMedium.heading(),
    labelSmall = baseline.labelSmall.heading(),
)

/** Screen-title size shared by every collapsing top bar (32sp/32sp). */
val ScreenTitleSize = 28.sp

/** Screen-title line height, matched to [ScreenTitleSize] for tight display leading. */
val ScreenTitleLineHeight = 28.sp
