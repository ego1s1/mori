package com.mori.feature.reader.impl

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.MoriMotion
import com.mori.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit) {
    composable<ReaderRoute>(
        enterTransition = { readerEnter() },
        exitTransition = { readerExit() },
        popEnterTransition = { readerEnter() },
        popExitTransition = { readerExit() },
    ) {
        ReaderRoute(onBackClick = onBackClick)
    }
}

/**
 * The reader opens onto a fullscreen black bed, so sliding it sideways over
 * the shelf reads as lag. A fast fade is seamless: the art simply appears.
 */
private fun readerEnter(): EnterTransition =
    fadeIn(animationSpec = MoriMotion.readerEnterSpec())

private fun readerExit(): ExitTransition =
    fadeOut(animationSpec = MoriMotion.readerExitSpec())
