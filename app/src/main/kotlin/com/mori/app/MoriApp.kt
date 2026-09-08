package com.mori.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mori.core.designsystem.MoriTheme
import com.mori.feature.detail.api.navigateToDetail
import com.mori.feature.detail.impl.detailScreen
import com.mori.feature.library.api.navigateToLibrary
import com.mori.feature.library.impl.libraryScreen
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.onboarding.impl.onboardingScreen
import com.mori.feature.reader.api.navigateToReader
import com.mori.feature.reader.impl.readerScreen

/**
 * App entry point: theme + top-level navigation.
 *
 * F1 will drive the start destination from onboarding state in DataStore; for now the app
 * always starts at onboarding with forward navigation into the placeholder screens.
 */
@Composable
fun MoriApp(modifier: Modifier = Modifier) {
    MoriTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = OnboardingRoute,
            ) {
                onboardingScreen(
                    onOnboardingComplete = { navController.navigateToLibrary() },
                )
                libraryScreen(
                    onComicClick = { navController.navigateToDetail(it) },
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
