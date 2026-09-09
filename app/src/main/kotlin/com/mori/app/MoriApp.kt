package com.mori.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.rememberSystemReduceMotion
import com.mori.core.designsystem.resolveExpressiveMotionEnabled
import com.mori.core.model.MotionStyle
import com.mori.core.model.ThemeMode
import com.mori.feature.detail.api.navigateToDetail
import com.mori.feature.detail.impl.detailScreen
import com.mori.feature.library.api.LibraryRoute
import com.mori.feature.library.api.navigateToLibrary
import com.mori.feature.library.impl.libraryScreen
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.onboarding.impl.onboardingScreen
import com.mori.feature.reader.api.navigateToReader
import com.mori.feature.reader.impl.readerScreen
import com.mori.feature.settings.api.SettingsRoute
import com.mori.feature.settings.api.navigateToSettings
import com.mori.feature.settings.impl.settingsScreen

/**
 * App entry point: theme + top-level navigation.
 *
 * Library taps open the reader directly at the saved page; long-press opens details.
 * Theme follows persisted preferences (system/light/dark + dynamic color + AMOLED).
 */
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
        amoled = theme?.amoled ?: false,
    ) {
        CompositionLocalProvider(
            LocalExpressiveMotionEnabled provides expressiveMotion,
        ) {
            Surface(modifier = modifier.fillMaxSize()) {
            val completed by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

            if (completed == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
                return@Surface
            }

            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()

            LaunchedEffect(completed, backStackEntry) {
                if (completed == true && backStackEntry?.destination?.route == OnboardingRoute::class.qualifiedName) {
                    navController.navigateToLibrary()
                }
            }

            NavHost(
                navController = navController,
                startDestination = if (completed == true) LibraryRoute else OnboardingRoute,
            ) {
                onboardingScreen(
                    onOnboardingComplete = { navController.navigateToLibrary() },
                )
                libraryScreen(
                    onReadClick = { comicId, pageIndex ->
                        navController.navigateToReader(comicId, pageIndex)
                    },
                    onComicLongClick = { navController.navigateToDetail(it) },
                    onSettingsClick = { navController.navigateToSettings() },
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
                settingsScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
    }
}
}
