package com.mori.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.LocalNavAnimatedVisibilityScope
import com.mori.core.designsystem.MoriMotion
import com.mori.core.model.ResumeTarget
import com.mori.feature.stats.impl.StatsTabContent
import com.mori.feature.library.impl.LibraryTabContent
import com.mori.feature.onboarding.api.OnboardingRoute
import com.mori.feature.settings.impl.SettingsTabContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Top-level main viewport: Library and Settings as bottom-nav tabs. */
@Serializable
object MainRoute

/**
 * Survives process death (unlike plain remember): the resume FAB stays
 * until the library flow re-emits, instead of vanishing after a kill.
 */
private val ResumeTargetSaver: Saver<ResumeTarget?, Any> = listSaver(
    save = { target ->
        if (target == null) emptyList() else listOf(target.comicId, target.pageIndex, target.title)
    },
    restore = { parts ->
        val comicId = parts.getOrNull(0) as? String
        val pageIndex = (parts.getOrNull(1) as? Number)?.toInt()
        val title = parts.getOrNull(2) as? String
        if (comicId == null || pageIndex == null || title == null) {
            null
        } else {
            ResumeTarget(
                comicId = comicId,
                pageIndex = pageIndex,
                title = title,
            )
        }
    },
)

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
    onLicensesClick: () -> Unit,
) {
    composable<MainRoute> {
        CompositionLocalProvider(
            LocalNavAnimatedVisibilityScope provides this,
        ) {
            MainScreen(
                onReadClick = onReadClick,
                onComicLongClick = onComicLongClick,
                onLicensesClick = onLicensesClick,
            )
        }
    }
}

/**
 * Main viewport: Library, Stats and Settings are separate tab destinations
 * under one floating navigator — no swipe pager. Tabs switch with a smooth
 * directional glide and each keeps its state (grid scroll position survives
 * a settings visit), so the heavy settings page never composes mid-gesture.
 * The system back gesture jumps home to the library instead of leaving.
 */
@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun MainScreen(
    onReadClick: (comicId: String, pageIndex: Int) -> Unit,
    onComicLongClick: (String) -> Unit,
    onLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()

    // Predictive-back preview for leaving to the library: the content leans
    // with the gesture (subtle pull + settle) instead of snapping on
    // release. Commit jumps straight home (one gesture exits settings);
    // cancel glides back to rest.
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val backPreview = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = selectedTab != LIBRARY_TAB) { progress ->
        try {
            progress.collect { backPreview.snapTo(it.progress) }
            selectedTab = LIBRARY_TAB
            backPreview.snapTo(0f)
        } catch (e: CancellationException) {
            // Settle on the motion setting: spring back expressively,
            // quiet fade-spec glide when calm.
            backPreview.animateTo(
                0f,
                animationSpec = if (expressiveMotion) {
                    MoriMotion.defaultSpatialSpec()
                } else {
                    MoriMotion.calmFade()
                },
            )
            throw e
        }
    }

    var resume by rememberSaveable(stateSaver = ResumeTargetSaver) { mutableStateOf<ResumeTarget?>(null) }

    // Reading consumes the resume cue: clear it alongside navigation so the
    // FAB never lingers over the reader or survives a return stale.
    val handleReadClick: (String, Int) -> Unit = { comicId, pageIndex ->
        resume = null
        onReadClick(comicId, pageIndex)
    }

    // Single floating navigator for all tabs (destinations + resume); the
    // library's action toolbar floats above it. No bottom bar.
    //
    // The navigator stays pinned: like the reference app, the scroll behavior
    // exists for the toolbar's internal animation contract but is never wired
    // to scroll input, so the pill can never rest half-sunk or stuck hidden.
    // (Hide-on-scroll + settle-snaps were tried; any rest state other than
    // fully shown reads as broken layout on a small floating pill.)
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        FloatingToolbarExitDirection.Bottom,
    )
    Scaffold(
        // Edge-to-edge: content draws behind the system bars while chrome
        // clears them. Only sides come from here — the top bar consumes the
        // status inset itself, and also taking Top doubled it (~52dp of dead
        // space above every title).
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // No entry animation here: the NavHost transition already carries
            // the arrival. A second scale-in stacked on top read as a glitch.
            // Tab travel is a directional glide (fixed-time tweens retarget
            // cleanly on rapid hops); calm motion crossfades instead.
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (!expressiveMotion) {
                        fadeIn(animationSpec = MoriMotion.calmFade()) togetherWith
                            fadeOut(animationSpec = MoriMotion.calmFade())
                    } else {
                        // Directional glide: entering content drifts in from
                        // the travel side while the old one recedes — a beat
                        // longer than screen chrome so tab travel reads as
                        // deliberate, subtler than a full slide.
                        val forward = targetState > initialState
                        val sign = if (forward) 1 else -1
                        (fadeIn(animationSpec = MoriMotion.tabEnterSpec()) +
                            slideInHorizontally(animationSpec = MoriMotion.tabEnterSpec()) { sign * it / 4 }) togetherWith
                            (fadeOut(animationSpec = MoriMotion.tabExitSpec()) +
                                slideOutHorizontally(animationSpec = MoriMotion.tabExitSpec()) { -sign * it / 4 })
                    }
                },
                label = "mainTabs",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pull = backPreview.value
                        translationX = pull * size.width * 0.08f
                        val settle = 1f - 0.02f * pull
                        scaleX = settle
                        scaleY = settle
                    },
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        LIBRARY_TAB -> LibraryTabContent(
                            onReadClick = handleReadClick,
                            onComicLongClick = onComicLongClick,
                            onResumeAvailable = { if (it != resume) resume = it },
                        )
                        STATS_TAB -> StatsTabContent()
                        else -> SettingsTabContent(
                            onLicensesClick = onLicensesClick,
                            appVersion = BuildConfig.VERSION_NAME,
                        )
                    }
                }
            }
            // No enter/exit animation: this bar is always on screen, and replaying
            // a slide-up whenever MainScreen re-enters composition (e.g. returning
            // from the reader) left it visibly low for a beat before it settled.
            // The nav-bar inset animates back in as the reader's hidden system
            // bars return; reading it live made the bar sit low for a beat and
            // snap up. Track the running maximum of the live inset so the bar
            // keeps its settled height across that transition.
            //
            // The source is navigationBars, never safeDrawing: safeDrawing
            // includes the IME, so opening search latched keyboard height and
            // parked the pill mid-screen until restart. The clamp below is
            // belt-and-braces so no inset spike can ever lift the bar again.
            val liveNavBottom = WindowInsets.navigationBars
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
                .calculateBottomPadding()
            val latchedNavBottom = rememberSaveable { mutableFloatStateOf(liveNavBottom.value) }
            SideEffect {
                if (liveNavBottom.value > latchedNavBottom.floatValue) {
                    latchedNavBottom.floatValue = liveNavBottom.value
                }
            }
            val navBottom = maxOf(liveNavBottom.value, latchedNavBottom.floatValue).dp
                .coerceAtMost(MaxChromeBottomInset)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + 16.dp),
            ) {
                // Pill + resume are centred together as one unit, so the
                // group's centroid sits on the screen centre.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    MainNavigator(
                        selectedTab = selectedTab,
                        onSelectTab = { tab ->
                            selectedTab = tab
                        },
                        scrollBehavior = toolbarScrollBehavior,
                    )
                    resume?.let { target ->
                        ResumeButton(
                            title = target.title,
                            onClick = { handleReadClick(target.comicId, target.pageIndex) },
                        )
                    }
                }
            }
        }
    }
}

private const val LIBRARY_TAB = 0
private const val STATS_TAB = 1
private const val SETTINGS_TAB = 2

/** Ceiling for the floating chrome offset: nav bars never need more. */
private val MaxChromeBottomInset = 120.dp
