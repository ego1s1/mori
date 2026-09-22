package com.mori.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * Shared navigation transitions: every destination enters/exits on the same
 * emphasized curves so screen changes carry one motion personality. Called
 * from NavHost transition lambdas, whose receiver is the content scope.
 *
 * NavHost lambdas are not composable, so they cannot read
 * [LocalExpressiveMotionEnabled]: callers thread the resolved setting through
 * [expressive] (MoriApp reads it at the NavHost call site). Calm motion
 * drops the lateral slide and keeps the fade.
 */
fun AnimatedContentTransitionScope<*>.screenEnter(expressive: Boolean = true): EnterTransition =
    if (!expressive) {
        fadeIn(animationSpec = MoriMotion.calmFade())
    } else {
        fadeIn(animationSpec = MoriMotion.screenEnterSpec()) + slideIntoContainer(
            animationSpec = MoriMotion.screenEnterSpec(),
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
        )
    }

fun AnimatedContentTransitionScope<*>.screenExit(expressive: Boolean = true): ExitTransition =
    if (!expressive) {
        fadeOut(animationSpec = MoriMotion.calmFade())
    } else {
        fadeOut(animationSpec = MoriMotion.screenExitSpec()) + slideOutOfContainer(
            animationSpec = MoriMotion.screenExitSpec(),
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
        )
    }

fun AnimatedContentTransitionScope<*>.screenPopEnter(expressive: Boolean = true): EnterTransition =
    if (!expressive) {
        fadeIn(animationSpec = MoriMotion.calmFade())
    } else {
        fadeIn(animationSpec = MoriMotion.screenEnterSpec()) + slideIntoContainer(
            animationSpec = MoriMotion.screenEnterSpec(),
            towards = AnimatedContentTransitionScope.SlideDirection.End,
        )
    }

fun AnimatedContentTransitionScope<*>.screenPopExit(expressive: Boolean = true): ExitTransition =
    if (!expressive) {
        fadeOut(animationSpec = MoriMotion.calmFade())
    } else {
        fadeOut(animationSpec = MoriMotion.screenExitSpec()) + slideOutOfContainer(
            animationSpec = MoriMotion.screenExitSpec(),
            towards = AnimatedContentTransitionScope.SlideDirection.End,
        )
    }

/**
 * Wizard-handoff enter: completing onboarding lands on Main with a fade, not
 * a lateral push — a forward completion reads as arrival, matching the
 * reader's "different bed = different transition" precedent.
 */
fun AnimatedContentTransitionScope<*>.wizardEnter(expressive: Boolean = true): EnterTransition =
    fadeIn(
        animationSpec = if (expressive) {
            MoriMotion.screenEnterSpec()
        } else {
            MoriMotion.calmFade()
        },
    )

/** Wizard-handoff exit: the onboarding screen dissolves as Main arrives. */
fun AnimatedContentTransitionScope<*>.wizardExit(expressive: Boolean = true): ExitTransition =
    fadeOut(
        animationSpec = if (expressive) {
            MoriMotion.screenExitSpec()
        } else {
            MoriMotion.calmFade()
        },
    )
