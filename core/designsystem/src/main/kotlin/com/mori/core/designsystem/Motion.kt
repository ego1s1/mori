package com.mori.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import com.mori.core.model.MotionStyle

/**
 * M3 Expressive motion tokens: physics springs for spatial changes, emphasized easings
 * for transitions. The full MotionScheme API needs material3 1.4+, so the skill's
 * spatial/effects spec system is implemented directly here with stable spring APIs.
 *
 * Speed table (per skill): fast = small components (switches, chips), default =
 * buttons/cards/chrome, slow = sheets/dialogs/navigation. Effects specs (color/alpha)
 * never bounce; spatial specs bounce lightly in expressive mode.
 */
object MoriMotion {
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    const val EnterScreenMs = 400
    const val ExitScreenMs = 200
    const val SharedTransitionMs = 500

    /** Spatial: small components (switches, chips, icon buttons). */
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessHigh,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    /** Spatial: buttons, cards, chrome, pager transforms. Light expressive bounce. */
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = 0.6f,
    )

    /** Spatial: sheets, dialogs, screen transitions. */
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessLow,
        dampingRatio = 0.7f,
    )

    /** Effects: instant feedback (press states, focus). */
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessHigh,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    /** Effects: selection, enabled states, scrims. */
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    /** Effects: theme and background washes. */
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    /** Gentle expressive spring for chrome entrances (bottom bars, sheets content). */
    fun <T> chromeSpring(): FiniteAnimationSpec<T> = defaultSpatialSpec()

    /** Playful expressive spring for hero moments (FABs, covers, toggles). */
    fun <T> heroSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Calm fallback: short emphasized fades with no physics. */
    fun <T> calmFade(): FiniteAnimationSpec<T> = tween(150, easing = Emphasized)

    /**
     * Page-turn glide: a short fixed-time slide that retargets cleanly when a
     * new turn interrupts it mid-flight, so rapid chains stay smooth instead
     * of piling up long springs.
     */
    fun pageTurnSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = PAGE_TURN_MS, easing = EmphasizedDecelerate)

    private const val PAGE_TURN_MS = 150

    @Composable
    fun enterTween() = tween<IntOffset>(EnterScreenMs, easing = EmphasizedDecelerate)

    @Composable
    fun exitTween() = tween<IntOffset>(ExitScreenMs, easing = EmphasizedAccelerate)
}

/**
 * Whether expressive motion (springs, stagger, shared elements) should run.
 * Provided near the root from user setting + system reduced-motion state.
 */
val LocalExpressiveMotionEnabled = staticCompositionLocalOf { true }

/**
 * Pure selector: expressive motion runs only when the user chose it AND the system
 * has not disabled animations. Unit-testable without Android.
 */
fun resolveExpressiveMotionEnabled(
    style: MotionStyle,
    systemReduceMotion: Boolean,
): Boolean = style == MotionStyle.EXPRESSIVE && !systemReduceMotion

/** Animator duration scale of 0 means the user disabled system animations. */
fun isSystemReduceMotionEnabled(animatorDurationScale: Float): Boolean =
    animatorDurationScale == 0f
