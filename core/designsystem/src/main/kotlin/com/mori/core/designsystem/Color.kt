package com.mori.core.designsystem

import androidx.compose.ui.graphics.Color

// Static fallback palette (used when dynamic color is off or unavailable on API < 31).
// Seeds chosen for a deep-ink gallery feel; components must still use colorScheme roles.

internal val MoriSeedLight = Color(0xFFD6C2FF)
internal val MoriOnSeedDark = Color(0xFF2E1B5E)
internal val MoriSeedContainerDark = Color(0xFF44307E)
internal val MoriOnSeedContainerDark = Color(0xFFE8DEFF)
internal val MoriSecondaryDark = Color(0xFFC9C0D9)
internal val MoriTertiaryDark = Color(0xFFEFB8C8)

internal val MoriSeedDark = Color(0xFF5B4396)
internal val MoriOnSeedLight = Color(0xFFFFFFFF)
internal val MoriSeedContainerLight = Color(0xFFE8DEFF)
internal val MoriOnSeedContainerLight = Color(0xFF1E1042)
internal val MoriSecondaryLight = Color(0xFF605A71)
internal val MoriTertiaryLight = Color(0xFF7E525E)

// Preset schemes (primary / onPrimary / container / onContainer / secondary /
// tertiary, light + dark). Hand-tuned M3-style tonal sets for when dynamic
// (wallpaper) color is off.
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
