package com.mori.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Mori shapes: M3 tokens with expressive large variants for sheets and hero containers.
 */
val MoriShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Top-only extra-large corners for bottom panels: the sheet language in one
 * code path instead of per-screen 28dp literals.
 */
val Shapes.topSheet: Shape
    get() = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
