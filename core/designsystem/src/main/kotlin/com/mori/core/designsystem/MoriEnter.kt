package com.mori.core.designsystem

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Motion-aware entrance/exit pairs. Expressive motion glides on springs;
 * calm motion (or system reduced motion) fades quietly. Read from
 * [LocalExpressiveMotionEnabled] so the settings toggle applies everywhere
 * without threading flags through state.
 */
enum class MoriEnterKind {
    /** Top chrome bars slide down into place. */
    CHROME_TOP,

    /** Bottom chrome bars slide up into place. */
    CHROME_BOTTOM,

    /** Floating toolbar rises from below. */
    TOOLBAR,

    /** Search field expands downward. */
    SEARCH,

    /** Resume FAB scales in from its center. */
    FAB,

    /** Full-screen welcome content rises gently. */
    RISE,

    /**
     * Tab/step switches: incoming fades through with a breath of scale while
     * outgoing fades, so destination changes read as designed even when the
     * entering page spends its first frames composing.
     */
    FADE_THROUGH,

    /** Plain fade for content arrivals (grids, detail bodies, counters). */
    FADE,
}

/** Entrance for [kind] under the current motion setting. */
@Composable
fun MoriMotion.enter(kind: MoriEnterKind): EnterTransition {
    val expressive = LocalExpressiveMotionEnabled.current
    return remember(expressive, kind) {
        if (!expressive) {
            return@remember fadeIn(animationSpec = calmFade())
        }
        when (kind) {
            MoriEnterKind.CHROME_TOP -> fadeIn(animationSpec = defaultEffectsSpec()) +
                slideInVertically(animationSpec = chromeSpring()) { -it / 2 }
            MoriEnterKind.CHROME_BOTTOM -> fadeIn(animationSpec = defaultEffectsSpec()) +
                slideInVertically(animationSpec = chromeSpring()) { it / 2 }
            MoriEnterKind.TOOLBAR -> fadeIn(animationSpec = defaultEffectsSpec()) +
                slideInVertically(animationSpec = chromeSpring()) { it / 2 }
            MoriEnterKind.SEARCH -> fadeIn(animationSpec = defaultEffectsSpec()) +
                expandVertically(animationSpec = chromeSpring())
            MoriEnterKind.FAB -> fadeIn(animationSpec = defaultEffectsSpec()) +
                scaleIn(animationSpec = heroSpring(), initialScale = 0.6f)
            MoriEnterKind.RISE -> fadeIn(animationSpec = defaultEffectsSpec()) +
                slideInVertically(animationSpec = chromeSpring()) { it / 4 }
            MoriEnterKind.FADE_THROUGH -> fadeIn(animationSpec = defaultEffectsSpec()) +
                scaleIn(animationSpec = defaultSpatialSpec(), initialScale = 0.98f)
            MoriEnterKind.FADE -> fadeIn(animationSpec = defaultEffectsSpec())
        }
    }
}

/** Exit matching [MoriMotion.enter]: reverse slide, quiet calm fade. */
@Composable
fun MoriMotion.exit(kind: MoriEnterKind): ExitTransition {
    val expressive = LocalExpressiveMotionEnabled.current
    return remember(expressive, kind) {
        if (!expressive) {
            return@remember fadeOut(animationSpec = calmFade())
        }
        when (kind) {
            MoriEnterKind.CHROME_TOP -> fadeOut(animationSpec = defaultEffectsSpec()) +
                slideOutVertically(animationSpec = chromeSpring()) { -it / 2 }
            MoriEnterKind.CHROME_BOTTOM -> fadeOut(animationSpec = defaultEffectsSpec()) +
                slideOutVertically(animationSpec = chromeSpring()) { it / 2 }
            MoriEnterKind.TOOLBAR -> fadeOut(animationSpec = defaultEffectsSpec()) +
                slideOutVertically(animationSpec = chromeSpring()) { it / 2 }
            MoriEnterKind.SEARCH -> fadeOut(animationSpec = defaultEffectsSpec()) +
                shrinkVertically(animationSpec = chromeSpring())
            MoriEnterKind.FAB -> fadeOut(animationSpec = defaultEffectsSpec()) +
                scaleOut(animationSpec = calmFade())
            MoriEnterKind.RISE -> fadeOut(animationSpec = defaultEffectsSpec()) +
                slideOutVertically(animationSpec = chromeSpring()) { it / 4 }
            MoriEnterKind.FADE_THROUGH -> fadeOut(animationSpec = defaultEffectsSpec())
            MoriEnterKind.FADE -> fadeOut(animationSpec = defaultEffectsSpec())
        }
    }
}
