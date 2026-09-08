package com.mori.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * M3 Expressive motion tokens for screen transitions.
 *
 * Component-level spring physics come from the Material3 expressive APIs where the BOM
 * provides them; these emphasized easings cover enter/exit/shared transitions.
 */
object MoriMotion {
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    const val EnterScreenMs = 400
    const val ExitScreenMs = 200
    const val SharedTransitionMs = 500

    @Composable
    fun enterTween() = tween<IntOffset>(EnterScreenMs, easing = EmphasizedDecelerate)

    @Composable
    fun exitTween() = tween<IntOffset>(ExitScreenMs, easing = EmphasizedAccelerate)
}
