package com.mori.core.designsystem

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import com.mori.core.model.MotionStyle

/**
 * M3 Expressive motion tokens: physics springs for spatial changes, emphasized easings
 * for transitions. The full MotionScheme API needs material3 1.4+, so the skill's
 * spatial/effects spec system is implemented directly here with stable spring APIs.
 *
 * Speed table (per skill): fast = small components (switches, chips), default =
 * buttons/cards/chrome, slow = sheets/dialogs/navigation. Effects specs (color/alpha)
 * never bounce; spatial specs bounce lightly in expressive mode.
 *
 * Duration standards enforced across the app (M3 transition table):
 *
 * | Use                              | Duration | Easing / spec        | Token            |
 * |----------------------------------|----------|----------------------|------------------|
 * | Screen enter                     | 400ms    | EmphasizedDecelerate | screenEnterSpec  |
 * | Screen exit                      | 200ms    | EmphasizedAccelerate | screenExitSpec   |
 * | Tab travel glide (retargetable)  | 450/300ms| EmphasizedDec/Acc    | tabEnter/ExitSpec|
 * | Step fade-through                | spring   | spatial + effects    | FADE_THROUGH     |
 * | Content arrival fades            | spring   | effects              | FADE             |
 * | Page-turn glide                  | 180ms    | EmphasizedDecelerate | pageTurnSpec     |
 * | Double-tap zoom glide            | 500ms    | EaseInOutQuad        | zoomSpec         |
 * | Pan fling glide                  | 500ms    | EaseOutQuad          | flingSpec        |
 * | Edge pan hop                     | 180ms    | EmphasizedDecelerate | pageTurnSpec     |
 * | Calm fallback (any fade)         | 200ms    | Emphasized           | calmFade         |
 * | Reader open/close fades          | 180/150ms| EmphasizedDec/Acc    | readerEnter/Exit |
 * | Cover launch morph               | 650ms    | EmphasizedDecelerate | coverMorphTransform |
 *
 * Tab travel deliberately uses fixed-time tweens instead of the FADE_THROUGH
 * spring: rapid tab hops retarget cleanly mid-flight, matching the pager
 * glide contract. Calm motion drops the slide and keeps the fade.
 *
 * Rules: no raw `tween`/`spring` durations outside this file — call sites use
 * these tokens or named constants beside the usage. Screen-level transitions
 * go through `MoriMotion.enter()`/`exit()` so the calm-motion setting (and
 * system reduced motion) applies everywhere.
 */
object MoriMotion {
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /**
     * Robert Penner ease-out quad: fast start, soft landing for pan flings
     * and fling glides.
     */
    val EaseOutQuad = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

    /**
     * Robert Penner ease-in-out quad: gentle both ends for double-tap zoom.
     */
    val EaseInOutQuad = Easing { fraction ->
        if (fraction < 0.5f) {
            2f * fraction * fraction
        } else {
            -1f + (4f - 2f * fraction) * fraction
        }
    }

    const val EnterScreenMs = 400
    const val ExitScreenMs = 200

    /** Spatial: buttons, cards, chrome, pager transforms. Light expressive bounce. */
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = 0.6f,
    )

    /** Effects: selection, enabled states, scrims. */
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = spring(
        stiffness = Spring.StiffnessMedium,
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
    fun <T> calmFade(): FiniteAnimationSpec<T> = tween(200, easing = Emphasized)

    /**
     * Page-turn glide: a short fixed-time slide that retargets cleanly when a
     * new turn interrupts it mid-flight, so rapid chains stay smooth instead
     * of piling up long springs.
     */
    fun pageTurnSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = PAGE_TURN_MS, easing = EmphasizedDecelerate)

    /** Double-tap zoom glide: fixed-time in-out quad. */
    fun zoomSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = DOUBLE_TAP_ZOOM_MS, easing = EaseInOutQuad)

    /**
     * In-out quad tween for combined zoom state (scale + offset driven as one
     * Animatable so retargets stay seamless).
     */
    fun <T> zoomStateSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = DOUBLE_TAP_ZOOM_MS, easing = EaseInOutQuad)

    /**
     * Pan fling glide: fixed-time ease-out quad over the velocity-projected
     * target. The target carries the decay, so no physical friction spec
     * is needed.
     */
    fun flingSpec(): FiniteAnimationSpec<Offset> =
        tween(durationMillis = FLING_GLIDE_MS, easing = EaseOutQuad)

    /** Reader route fades: the fullscreen bed makes slides read as lag. */
    fun readerEnterSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = READER_FADE_IN_MS, easing = EmphasizedDecelerate)

    /** Reader route fades: the fullscreen bed makes slides read as lag. */
    fun readerExitSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = READER_FADE_OUT_MS, easing = EmphasizedAccelerate)

    /**
     * Cover launch morph: the shared element glides (and scales, where the
     * card and hero aspects differ) a beat slower than screen chrome, so the
     * book visibly travels instead of snapping.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun coverMorphTransform(): BoundsTransform =
        BoundsTransform { _, _ ->
            tween(durationMillis = COVER_MORPH_MS, easing = EmphasizedDecelerate)
        }

    /**
     * Tab travel glide: a beat longer than screen chrome so switching tabs
     * reads as deliberate travel, not a snap. Fixed-time tweens (not
     * springs) so rapid tab hops retarget cleanly mid-flight, matching the
     * pager glide contract.
     */
    fun <T> tabEnterSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = TAB_ENTER_MS, easing = EmphasizedDecelerate)

    /** Tab travel exit: quicker than enter so the arrival leads. */
    fun <T> tabExitSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = TAB_EXIT_MS, easing = EmphasizedAccelerate)

    /**
     * Screen-route slide+fade: fixed-time emphasized tweens (M3 sanctions
     * easing/duration for transitions; springs drive components, not routes).
     * Calm callers pass [expressive] = false and get fade-only.
     */
    fun <T> screenEnterSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = EnterScreenMs, easing = EmphasizedDecelerate)

    /** Screen-route exit: quicker than enter so the arrival leads. */
    fun <T> screenExitSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ExitScreenMs, easing = EmphasizedAccelerate)

    private const val PAGE_TURN_MS = 180
    private const val DOUBLE_TAP_ZOOM_MS = 500
    private const val FLING_GLIDE_MS = 500
    private const val READER_FADE_IN_MS = 180
    private const val READER_FADE_OUT_MS = 150
    private const val COVER_MORPH_MS = 650
    private const val TAB_ENTER_MS = 450
    private const val TAB_EXIT_MS = 300
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
