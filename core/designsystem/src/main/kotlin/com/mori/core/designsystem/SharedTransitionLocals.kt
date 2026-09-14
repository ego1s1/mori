package com.mori.core.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-element transition scopes for the cover launch (library card to
 * detail hero). Provided once around the NavHost; destinations expose the
 * animated-visibility scope per destination. Null outside navigation (tests,
 * previews) where shared elements degrade to plain layouts.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Stable shared key for a comic's cover across library and detail. */
fun comicCoverSharedKey(comicId: String): String = "cover-$comicId"

/**
 * Shared-element modifier morphing a comic cover between shelf and detail.
 * Degrades to a plain modifier outside navigation (tests, previews).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCoverModifier(comicId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || animatedScope == null) return Modifier
    return with(sharedScope) {
        Modifier.sharedElement(
            rememberSharedContentState(comicCoverSharedKey(comicId)),
            animatedScope,
            boundsTransform = MoriMotion.coverMorphTransform(),
        )
    }
}
