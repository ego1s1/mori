package com.mori.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Expressive emphasized twins to every [MoriTypography] role.
 *
 * Per M3 Expressive, the scale carries 15 baseline and 15 emphasized styles
 * meant to be used together; emphasized is reserved for selection, actions,
 * headlines, and editorial emphasis — never whole screens. Sizes, line
 * heights, and tracking are inherited verbatim from the baseline role, so an
 * emphasized twin only changes weight and family.
 */
private val base = Typography()

private fun TextStyle.emphasized(weight: FontWeight = FontWeight.Bold) =
    copy(fontFamily = HeadingFlex, fontWeight = weight)

object MoriEmphasized {
    val displayLarge = base.displayLarge.emphasized()
    val displayMedium = base.displayMedium.emphasized()
    val displaySmall = base.displaySmall.emphasized()
    val headlineLarge = base.headlineLarge.emphasized()
    val headlineMedium = base.headlineMedium.emphasized()
    val headlineSmall = base.headlineSmall.emphasized()
    val titleLarge = base.titleLarge.emphasized()
    val titleMedium = base.titleMedium.emphasized()
    val titleSmall = base.titleSmall.emphasized()
    val bodyLarge = base.bodyLarge.emphasized(FontWeight.SemiBold)
    val bodyMedium = base.bodyMedium.emphasized(FontWeight.SemiBold)
    val bodySmall = base.bodySmall.emphasized(FontWeight.SemiBold)
    val labelLarge = base.labelLarge.emphasized()
    val labelMedium = base.labelMedium.emphasized()
    val labelSmall = base.labelSmall.emphasized()
}
