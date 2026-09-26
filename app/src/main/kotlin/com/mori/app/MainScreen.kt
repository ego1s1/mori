package com.mori.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.model.ResumeTarget
import com.mori.feature.history.impl.HistoryTabContent
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
 * Main viewport: Library, History and Settings are separate tab destinations
 * under one floating navigator — no swipe pager. Tabs switch with a smooth
 * directional glide and each keeps its state (grid scroll position survives
 * a settings visit), so the heavy settings page never composes mid-gesture.
 * The system back gesture jumps home to the library instead of leaving.
 */
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
                        HISTORY_TAB -> HistoryTabContent(
                            onReadClick = handleReadClick,
                            onComicLongClick = onComicLongClick,
                        )
                        else -> SettingsTabContent(
                            onLicensesClick = onLicensesClick,
                            appVersion = BuildConfig.VERSION_NAME,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = MoriMotion.enter(MoriEnterKind.TOOLBAR),
                exit = MoriMotion.exit(MoriEnterKind.TOOLBAR),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MainNavigator(
                        selectedTab = selectedTab,
                        onSelectTab = { tab ->
                            selectedTab = tab
                        },
                    )
                    AnimatedVisibility(
                        visible = resume != null,
                        enter = MoriMotion.enter(MoriEnterKind.FAB),
                        exit = MoriMotion.exit(MoriEnterKind.FAB),
                    ) {
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
}

private const val LIBRARY_TAB = 0
private const val HISTORY_TAB = 1
private const val SETTINGS_TAB = 2
