package com.mori.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    // mirroring the reader's exit gesture. Disabled mid-swipe so the pager
    // and the gesture never fight over the page.
    var tabBackProgress by remember { mutableFloatStateOf(0f) }
    val backScope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = selectedTab == SETTINGS_TAB && !pagerState.isScrollInProgress) { progress ->
        try {
            progress.collect { event -> tabBackProgress = event.progress }
            tabBackProgress = 0f
            selectedTab = LIBRARY_TAB
        } catch (_: CancellationException) {
            // Ease back instead of snapping: cancelled gestures spring home.
            val start = tabBackProgress
            backScope.launch {
                animate(
                    initialValue = start,
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = MoriMotion.EmphasizedDecelerate,
                    ),
                ) { value, _ -> tabBackProgress = value }
            }
        }
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
            // Bouncy spring entry the first time home appears.
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = MoriMotion.defaultEffectsSpec()) +
                    scaleIn(
                        animationSpec = MoriMotion.heroSpring(),
                        initialScale = 0.92f,
                    ),
                modifier = Modifier.fillMaxSize(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                    LIBRARY_TAB -> LibraryTabContent(
                        onReadClick = onReadClick,
                        onComicLongClick = onComicLongClick,
                        onResumeAvailable = { resume = it },
                    )
                    else -> androidx.compose.foundation.layout.Box(
                        modifier = Modifier.graphicsLayer {
                            // Emphasized easing so the dip matches NavHost personality.
                            val p = MoriMotion.EmphasizedDecelerate.transform(
                                tabBackProgress.coerceIn(0f, 1f),
                            )
                            val scale = 1f - 0.08f * p
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - 0.25f * p
                        },
                    ) {
                            SettingsTabContent()
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = MoriMotion.enter(MoriEnterKind.TOOLBAR),
                exit = fadeOut(animationSpec = MoriMotion.calmFade()),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
