package com.mori.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.LocalNavAnimatedVisibilityScope
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
import com.mori.core.model.ResumeTarget
import com.mori.feature.library.impl.LibraryTabContent
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.settings.impl.SettingsTabContent
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

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.mainScreen(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
) {
    composable<MainRoute> {
        CompositionLocalProvider(
            LocalNavAnimatedVisibilityScope provides this,
        ) {
            MainScreen(
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
            )
        }
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

    // Tab bar -> pager. Guarded so a tap never fights an in-progress swipe.
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }
    // Swipe -> tab bar.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedTab = page
        }
    }

    // Predictive back on Settings returns to Library. No preview transform:
    // wrapping the whole settings tree in a graphicsLayer forced a full
    // offscreen buffer during swipes. Disabled mid-swipe so the pager and
    // the gesture never fight over the page.
    PredictiveBackHandler(enabled = selectedTab == SETTINGS_TAB && !pagerState.isScrollInProgress) { progress ->
        progress.collect { }
        selectedTab = LIBRARY_TAB
    }

    var resume by remember { mutableStateOf<ResumeTarget?>(null) }

    // Single floating navigator for both tabs (destinations + resume); the
    // library's action toolbar floats above it. No bottom bar.
    Scaffold(
        // Edge-to-edge bottom: content draws behind the system nav bar while
        // both floating elements clear it; top and sides stay inset.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // No entry animation here: the NavHost transition already carries
            // the arrival. A second scale-in stacked on top read as a glitch.
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    LIBRARY_TAB -> LibraryTabContent(
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onResumeAvailable = { resume = it },
                    )
                    else -> SettingsTabContent()
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = MoriMotion.enter(MoriEnterKind.TOOLBAR),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = 16.dp),
            ) {
                MainNavigator(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        selectedTab = tab
                    },
                    resume = resume,
                    onResumeClick = {
                        resume?.let { onReadClick(it.comicId, it.pageIndex) }
                    },
                )
            }
        }
    }
}

private const val TAB_COUNT = 2
private const val LIBRARY_TAB = 0
private const val SETTINGS_TAB = 1
