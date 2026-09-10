package com.mori.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * Shared navigation transitions: every destination enters/exits on the same
 * emphasized curves so screen changes carry one motion personality. Called
 * from NavHost transition lambdas, whose receiver is the content scope.
 */
fun AnimatedContentTransitionScope<*>.screenEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(
                MoriMotion.EnterScreenMs,
                easing = MoriMotion.EmphasizedDecelerate,
            ),
        ) + slideIntoContainer(
            animationSpec = tween(
                MoriMotion.EnterScreenMs,
                easing = MoriMotion.EmphasizedDecelerate,
            ),
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
        )

fun AnimatedContentTransitionScope<*>.screenExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(
                MoriMotion.ExitScreenMs,
                easing = MoriMotion.EmphasizedAccelerate,
            ),
        ) + slideOutOfContainer(
            animationSpec = tween(
                MoriMotion.ExitScreenMs,
                easing = MoriMotion.EmphasizedAccelerate,
            ),
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
        )

fun AnimatedContentTransitionScope<*>.screenPopEnter(): EnterTransition =
        fadeIn(
            animationSpec = tween(
                MoriMotion.EnterScreenMs,
                easing = MoriMotion.EmphasizedDecelerate,
            ),
        ) + slideIntoContainer(
            animationSpec = tween(
                MoriMotion.EnterScreenMs,
                easing = MoriMotion.EmphasizedDecelerate,
            ),
            towards = AnimatedContentTransitionScope.SlideDirection.End,
        )

fun AnimatedContentTransitionScope<*>.screenPopExit(): ExitTransition =
        fadeOut(
            animationSpec = tween(
                MoriMotion.ExitScreenMs,
                easing = MoriMotion.EmphasizedAccelerate,
            ),
        ) + slideOutOfContainer(
            animationSpec = tween(
                MoriMotion.ExitScreenMs,
                easing = MoriMotion.EmphasizedAccelerate,
            ),
            towards = AnimatedContentTransitionScope.SlideDirection.End,
        )
