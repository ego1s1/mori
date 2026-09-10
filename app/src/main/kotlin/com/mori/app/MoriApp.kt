package com.mori.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.screenEnter
import com.mori.core.designsystem.screenExit
import com.mori.core.designsystem.screenPopEnter
import com.mori.core.designsystem.screenPopExit
import com.mori.core.designsystem.LocalSharedTransitionScope
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.rememberSystemReduceMotion
import com.mori.core.designsystem.resolveExpressiveMotionEnabled
import com.mori.core.model.ColorSchemeChoice
import com.mori.core.model.MotionStyle
import com.mori.core.model.ThemeMode
import com.mori.feature.detail.api.navigateToDetail
import com.mori.feature.detail.impl.detailScreen
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.onboarding.impl.onboardingScreen
import com.mori.feature.reader.api.navigateToReader
import com.mori.feature.reader.impl.readerScreen

/**
 * App entry point: theme + top-level navigation.
 *
 * Library taps open the reader directly at the saved page; long-press opens details.
 * Theme follows persisted preferences (system/light/dark + dynamic color + AMOLED).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoriApp(
    modifier: Modifier = Modifier,
    viewModel: MoriAppViewModel = hiltViewModel(),
) {
    val theme = viewModel.themePreferences.collectAsStateWithLifecycle().value
    val motionStyle by viewModel.motionStyle.collectAsStateWithLifecycle()
    val darkTheme = when (theme?.mode ?: ThemeMode.SYSTEM) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val expressiveMotion = resolveExpressiveMotionEnabled(
        style = motionStyle ?: MotionStyle.EXPRESSIVE,
        systemReduceMotion = rememberSystemReduceMotion(),
    )
    MoriTheme(
        darkTheme = darkTheme,
        dynamicColor = theme?.dynamicColor ?: true,
        colorScheme = theme?.colorScheme ?: ColorSchemeChoice.MORI,
        amoled = theme?.amoled ?: false,
    ) {
        CompositionLocalProvider(
            LocalExpressiveMotionEnabled provides expressiveMotion,
        ) {
            Surface(modifier = modifier.fillMaxSize()) {
            val completed by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

            if (completed == null) {
                MoriLoading()
                return@Surface
            }

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()

            LaunchedEffect(completed, backStackEntry) {
                if (completed == true && backStackEntry?.destination?.route == OnboardingRoute::class.qualifiedName) {
                    navController.navigateToMain()
                }
            }

            // Screen transitions ride the shared emphasized curves so every
            // destination enters/exits with the same motion personality.
            // Covers additionally morph between shelf and detail (shared
            // element) inside this layout.
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (completed == true) MainRoute else OnboardingRoute,
                        enterTransition = { screenEnter() },
                        exitTransition = { screenExit() },
                        popEnterTransition = { screenPopEnter() },
                        popExitTransition = { screenPopExit() },
                    ) {
                        onboardingScreen(
                            onOnboardingComplete = { navController.navigateToMain() },
                        )
                        mainScreen(
                            onReadClick = { comicId, pageIndex ->
                                navController.navigateToReader(comicId, pageIndex)
                            },
                            onComicLongClick = { navController.navigateToDetail(it) },
                        )
                        detailScreen(
                            onBackClick = { navController.popBackStack() },
                            onReadClick = { comicId, pageIndex ->
                                navController.navigateToReader(comicId, pageIndex)
                            },
                        )
                        readerScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
}
