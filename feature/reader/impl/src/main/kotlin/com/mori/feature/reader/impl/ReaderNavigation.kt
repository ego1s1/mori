package com.mori.feature.reader.impl

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.MoriMotion
import com.mori.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit, expressive: Boolean = true) {
    composable<ReaderRoute>(
        enterTransition = { readerEnter(expressive) },
        exitTransition = { readerExit(expressive) },
        popEnterTransition = { readerEnter(expressive) },
        popExitTransition = { readerExit(expressive) },
    ) {
        ReaderRoute(onBackClick = onBackClick)
    }
}

/**
 * The reader opens onto a fullscreen black bed, so sliding it sideways over
 * the shelf reads as lag. A fast fade is seamless: the art simply appears.
 * Calm motion keeps the fade (already quiet) on the calm curve.
 */
private fun readerEnter(expressive: Boolean): EnterTransition =
    if (expressive) {
        fadeIn(animationSpec = MoriMotion.readerEnterSpec())
    } else {
        fadeIn(animationSpec = MoriMotion.calmFade())
    }

private fun readerExit(expressive: Boolean): ExitTransition =
    if (expressive) {
        fadeOut(animationSpec = MoriMotion.readerExitSpec())
    } else {
        fadeOut(animationSpec = MoriMotion.calmFade())
    }
