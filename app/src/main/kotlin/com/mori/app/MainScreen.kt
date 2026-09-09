package com.mori.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.MoriIcons
import com.mori.feature.library.impl.LibraryTabContent
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.settings.impl.SettingsTabContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Top-level main viewport: Library and Settings as bottom-nav tabs. */
@Serializable
object MainRoute

fun NavController.navigateToMain() {
    navigate(MainRoute) {
        popUpTo(OnboardingRoute) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavGraphBuilder.mainScreen(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
) {
    composable<MainRoute> {
        MainScreen(
            onReadClick = onReadClick,
            onComicLongClick = onComicLongClick,
        )
    }
}

/**
 * Main viewport: Library and Settings live side by side in a swipeable pager
 * under a bottom navigation bar, so settings opens inside the main viewport
 * with navigation always visible. Tabs switch via bar or swipe; the system
 * back gesture on the Settings tab returns to Library instead of leaving.
 */
@Composable
internal fun MainScreen(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val scope = rememberCoroutineScope()

    // Tab bar -> pager.
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    // Swipe -> tab bar.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTab = page
        }
    }

    // Predictive back on Settings returns to Library with a dip preview,
    // mirroring the reader's exit gesture.
    var tabBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = selectedTab == SETTINGS_TAB) { progress ->
        try {
            progress.collect { event -> tabBackProgress = event.progress }
            tabBackProgress = 0f
            selectedTab = SETTINGS_TAB - 1
        } catch (_: CancellationException) {
            tabBackProgress = 0f
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == LIBRARY_TAB,
                    onClick = { selectedTab = LIBRARY_TAB },
                    icon = {
                        Icon(
                            imageVector = MoriIcons.GridView,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Library") },
                )
                NavigationBarItem(
                    selected = selectedTab == SETTINGS_TAB,
                    onClick = { selectedTab = SETTINGS_TAB },
                    icon = {
                        Icon(
                            imageVector = MoriIcons.Settings,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Settings") },
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when (page) {
                LIBRARY_TAB -> LibraryTabContent(
                    onReadClick = onReadClick,
                    onComicLongClick = onComicLongClick,
                    onSettingsClick = {
                        scope.launch { pagerState.animateScrollToPage(SETTINGS_TAB) }
                    },
                )
                else -> androidx.compose.foundation.layout.Box(
                    modifier = Modifier.graphicsLayer {
                        val p = tabBackProgress.coerceIn(0f, 1f)
                        val scale = 1f - 0.08f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.25f * p
                    },
                ) {
                    SettingsTabContent(
                        onBackClick = {
                            scope.launch { pagerState.animateScrollToPage(LIBRARY_TAB) }
                        },
                    )
                }
            }
        }
    }
}

private const val TAB_COUNT = 2
private const val LIBRARY_TAB = 0
private const val SETTINGS_TAB = 1
