package com.mori.feature.reader.impl

import android.view.KeyEvent
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriComicErrorCard
import com.mori.core.designsystem.MoriErrorCard
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriScrimPill
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection
import com.mori.feature.reader.api.ReaderKeyInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
internal fun ReaderRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val latestState = rememberUpdatedState(uiState)
    val latestAction = rememberUpdatedState(viewModel::onAction)
    // Activity-level volume handling lives here (not in composition focus):
    // while the reader is visible, MainActivity offers every key event to
    // this handler before the system sees it.
    DisposableEffect(Unit) {
        val ours: (KeyEvent) -> Boolean = { event ->
            when (val outcome = routeVolumeKey(latestState.value, event.keyCode, event.action)) {
                VolumeKeyOutcome.Ignored -> false
                VolumeKeyOutcome.Consumed -> true
                is VolumeKeyOutcome.Navigate -> {
                    latestAction.value(outcome.action)
                    true
                }
            }
        }
        ReaderKeyInterceptor.handler = ours
        onDispose {
            // CAS-null: stacked readers must not wipe each other's handler —
            // only clear if ours is still installed.
            if (ReaderKeyInterceptor.handler === ours) {
                ReaderKeyInterceptor.handler = null
            }
        }
    }
    ReaderScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun ReaderScreen(
    uiState: ReaderUiState,
    onAction: (ReaderAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        // Pure-black bed: pages and letterboxing assume Black, so a themed
        // translucent scrim would seam mid-turn and at the well edges.
        color = Color.Black,
    ) {
        when (uiState) {
            ReaderUiState.Loading -> MoriLoading()

            is ReaderUiState.Error -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(24.dp),
            ) {
                when (val cause = uiState.cause) {
                    ReaderErrorCause.Removed -> MoriErrorCard(
                        body = stringResource(R.string.reader_error_removed),
                        primaryLabel = stringResource(R.string.reader_back_to_library),
                        onPrimary = onBackClick,
                    )

                    is ReaderErrorCause.Failed -> MoriComicErrorCard(
                        error = cause.error,
                        secondaryLabel = stringResource(R.string.reader_back_to_library),
                        onSecondary = onBackClick,
                    )
                }
            }

            is ReaderUiState.Ready -> ReaderContent(
                state = uiState,
                onAction = onAction,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState.Ready,
    onAction: (ReaderAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.pageIndex,
        pageCount = { state.pageCount },
    )
    // Viewer pages hoisted once per composition so slot lookup stays O(1);
    // pager content is keyed by the list instance below (split/direction/
    // invert/scan rebuilds remount pages at fit).
    val pages = state.viewerPages

    // ViewModel -> pager (buttons, taps, slider, seeks). Turns glide on a
    // short retargeting spec: each new target cancels the in-flight glide and
    // restarts from the live offset, so rapid chains stay smooth and lossless.
    // Slider seeks and calm motion jump instantly (direct manipulation, no
    // animation to interrupt). Swipes keep their native gesture animation.
    // The target is clamped to the live count and the effect keys on it: a
    // split/direction rebuild mid-glide cancels the stale animation instead
    // of retargeting out of range.
    val pagerExpressive = LocalExpressiveMotionEnabled.current
    LaunchedEffect(state.pageIndex, state.pageCount, state.turnAnimated, pagerExpressive) {
        val target = state.pageIndex.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
        // Skip while settling: a swipe-driven offset is already converging on
        // the target; retargeting mid-settle would fight the gesture.
        if (pagerState.currentPage != target && pagerState.currentPageOffsetFraction == 0f) {
            if (pagerExpressive && state.turnAnimated) {
                pagerState.animateScrollToPage(
                    target,
                    animationSpec = MoriMotion.pageTurnSpec(),
                )
            } else {
                pagerState.scrollToPage(target)
            }
        }
    }
    // Pager -> ViewModel (swipes).
    // Zoom/pan ownership (edge handoff): the page pans while its content
    // has room; a swipe starting already clamped at the edge passes
    // straight through untouched, so the pager drags it natively with the
    // finger still down. Pans that reach the clamp mid-gesture turn through
    // the explicit overshoot dispatch. The pager only ever stands down for
    // multi-touch: pinches belong to the page outright. A settled page is
    // always at fit, so any pager move resets the gate.
    var pinching by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
            onAction(ReaderAction.PageChanged(page))
        }
    }

    val rtl = state.direction == ReadingDirection.RIGHT_TO_LEFT
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sliderInteraction = remember { MutableInteractionSource() }
    val scrubbing by sliderInteraction.collectIsDraggedAsState()
    val contentScope = rememberCoroutineScope()
    // Detector epoch for the container taps: a direction flip bumps it so a
    // double-tap hold parked across the flip drops instead of dispatching
    // with the old zone. Starts at 1 — the launch effect below runs on first
    // composition too, and 0 must never read as a valid stamp.
    val tapEpoch = remember { mutableIntStateOf(1) }
    LaunchedEffect(state.direction) { tapEpoch.intValue++ }

    // Predictive-back preview: the bed shrinks with the gesture instead of
    // snapping on release; commit leaves the reader, cancel settles back.
    // Sheets own the back press while open, so the handler stands down then.
    val backPreview = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = !state.settingsOpen && !state.overviewOpen) { progress ->
        try {
            progress.collect { backPreview.snapTo(it.progress) }
            onBackClick()
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                backPreview.animateTo(
                    0f,
                    animationSpec = if (pagerExpressive) {
                        MoriMotion.defaultSpatialSpec()
                    } else {
                        MoriMotion.calmFade()
                    },
                )
            }
            throw e
        }
    }

    // Back exits through the NavHost: Navigation Compose scrubs the pop
    // transitions with the system gesture, so the library shows through for
    // real. No custom handler here — consuming the press would block that and
    // the settings sheet dismisses itself first via its own back handling.

    // Chrome interaction epoch: bumped by chrome control callbacks
    // (direction/fit/crop/settings/overview/bookmark) so the auto-hide timer
    // below restarts on any chrome interaction, not just page turns.
    var chromeInteractionEpoch by remember { mutableIntStateOf(0) }
    // Mirrors ReaderBottomChrome's local `scrub != null` (slider held but drag
    // detection lagging); updated via onScrubChange below.
    var chromeScrubHeld by remember { mutableStateOf(false) }
    val onChromeAction: (ReaderAction) -> Unit = { action ->
        when (action) {
            is ReaderAction.SetDirection, is ReaderAction.SetPageFit,
            ReaderAction.ToggleCrop, ReaderAction.OpenSettings,
            ReaderAction.OpenOverview, ReaderAction.ToggleBookmark -> chromeInteractionEpoch++
            else -> Unit
        }
        onAction(action)
    }

    // Auto-hide chrome after a moment of stillness, but never mid-scrub.
    if (state.chromeVisible && !state.settingsOpen && !state.overviewOpen && !scrubbing && !chromeScrubHeld) {
        LaunchedEffect(state.chromeVisible, state.pageIndex, chromeInteractionEpoch) {
            delay(CHROME_AUTO_HIDE_MS)
            onAction(ReaderAction.HideChrome)
        }
    }
    // Tactile ticks while scrubbing through pages.
    LaunchedEffect(state.pageIndex, scrubbing) {
        if (scrubbing) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Single zone dispatcher shared by pages and the container fallback below,
    // so every tap resolves exactly once.
    val handleZone: (ReaderZone) -> Unit = { zone ->
        when (zone) {
            ReaderZone.PREV -> onAction(ReaderAction.PrevPage)
            ReaderZone.NEXT -> onAction(ReaderAction.NextPage)
            ReaderZone.MENU -> onAction(ReaderAction.ToggleChrome)
        }
    }

    // Chrome entrances follow the motion setting: expressive springs, calm fades.
    val topChromeEnter = MoriMotion.enter(MoriEnterKind.CHROME_TOP)
    val topChromeExit = MoriMotion.exit(MoriEnterKind.CHROME_TOP)
    val bottomChromeEnter = MoriMotion.enter(MoriEnterKind.CHROME_BOTTOM)
    val bottomChromeExit = MoriMotion.exit(MoriEnterKind.CHROME_BOTTOM)

    if (state.keepScreenOn) {
        DisposableEffect(context) {
            val window = (context as? android.app.Activity)?.window
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Fullscreen follows chrome: bars hide with the controls for true immersion,
    // return with them. Transient swipe still reveals bars temporarily (system).
    // Hide is delayed until chrome settles; show stays instant.
    LaunchedEffect(context, state.chromeVisible) {
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (state.chromeVisible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            delay(CHROME_SETTLE_DELAY_MS)
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(context) {
        onDispose {
            val window = (context as? android.app.Activity)?.window
            window?.let { WindowCompat.getInsetsController(it, it.decorView) }
                ?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        // Volume keys are handled at the activity level (ReaderRoute registers
        // with ReaderKeyInterceptor), so no focus juggling lives here.
        modifier = modifier
            .fillMaxSize(),
    ) {
        // Constrain the page well on expanded windows (M3 guidance caps gallery
        // content around 840dp); phones stay full-bleed.
        androidx.compose.foundation.layout.BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Container-level fallback: taps that miss every page (black bed
            // between pages mid page-turn, letterbox margins on wide screens)
            // still resolve to a zone. Page taps are consumed by the pages
            // themselves, and chrome taps by their clickables, so each tap
            // dispatches exactly once. Mirrors the pager-level detection in
            // the reference reader, where taps resolve against the viewport
            // rather than individual page views.
            val density = LocalDensity.current
            val containerWidthPx = remember(density, maxWidth) {
                with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            }
            val viewportWidth = rememberUpdatedState(containerWidthPx)
            val pageWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pull = backPreview.value
                        val settle = 1f - BACK_PREVIEW_SHRINK * pull
                        scaleX = settle
                        scaleY = settle
                        alpha = 1f - BACK_PREVIEW_FADE * pull
                    }
                    .zoneTaps(
                        viewportWidth = viewportWidth,
                        direction = state.direction,
                        scope = contentScope,
                        epoch = tapEpoch,
                        onZoneTap = handleZone,
                        onZoom = { _, _ -> handleZone(ReaderZone.MENU) },
                        consumeUp = false,
                    ),
            ) {
                val pagerDescription =
                    stringResource(R.string.reader_pager_description, state.currentPage, state.pageCount)
                val nextLabel = stringResource(R.string.reader_next_page)
                val prevLabel = stringResource(R.string.reader_previous_page)
                HorizontalPager(
                    state = pagerState,
                    reverseLayout = rtl,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = state.swipeToTurn && !pinching,
                    modifier = Modifier
                        .width(pageWidth)
                        .fillMaxHeight()
                        .testTag(ReaderTestTags.Pager)
                        .semantics {
                            contentDescription = pagerDescription
                            customActions = listOf(
                                androidx.compose.ui.semantics.CustomAccessibilityAction(nextLabel) {
                                    onAction(ReaderAction.NextPage)
                                    true
                                },
                                androidx.compose.ui.semantics.CustomAccessibilityAction(prevLabel) {
                                    onAction(ReaderAction.PrevPage)
                                    true
                                },
                            )
                        },
                ) { page ->
                    // Neighbor ease: a whisper of shrink, no fade. Alpha on a
                    // black bed reads as flicker during fast swipes; the pager
                    // owns swipe physics natively, so chrome adds only shape.
                    // pageOffset is read inside graphicsLayer only (layout phase),
                    // never at composition level, to avoid recomposing on scroll.
                    // Dual-page split: the pager walks expanded positions, each
                    // resolving to an archive page plus the half to decode.
                    val viewerPage = pages.getOrNull(page)
                        ?: ReaderViewerPage(page, PageHalf.FULL)
                    // Remount on list rebuilds (split/direction/invert/scan):
                    // zoom and pan belong to a layout, not a slot — the page
                    // remembers zoom by archive identity, so carrying it
                    // across a rebuild would strand zoom on a page it no
                    // longer describes. Fresh pages start at fit. Keyed by list
                    // content directly (no hash Int intermediary).
                    key(state.viewerPages) {
                        ZoomablePage(
                        comicId = state.comicId,
                        pageIndex = viewerPage.archiveIndex,
                        pageNumber = viewerPage.archiveIndex + 1,
                        pageFit = state.pageFit,
                        direction = state.direction,
                        cropMargins = state.cropMargins,
                        half = viewerPage.half,
                        displayFilter = state.displayFilter,
                        swipeToTurn = state.swipeToTurn,
                        onPinchingChange = { pinching = it },
                        onEdgeTurn = { forward ->
                            // swipeToTurn off means swipes never turn — taps own that.
                            if (state.swipeToTurn) {
                                onAction(if (forward) ReaderAction.NextPage else ReaderAction.PrevPage)
                            }
                        },
                        modifier = Modifier.graphicsLayer {
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).absoluteValue
                            val scale = 1f - (pageOffset * PAGE_SHRINK).coerceIn(0f, PAGE_SHRINK)
                            scaleX = scale
                            scaleY = scale
                        },
                        onZoneTap = handleZone,
                    )
                    }
                }
            }
        }

        // Preview hides while sheets are open: it would tint the art
        // behind the settings being adjusted.
        if (state.showTapZones && !state.settingsOpen && !state.overviewOpen) {
            TapZoneOverlay(direction = state.direction)
        }

        // Traversal note: Modifier.traversalIndex (1.8 API) is unavailable here —
        // skipped deliberately. Composition order is left unchanged (reordering
        // chrome before pager would be risky for gesture/tap precedence).
        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = topChromeEnter,
            exit = topChromeExit,
        ) {
            ReaderTopBar(
                title = state.title,
                subtitle = state.subtitle,
                bookmarked = state.bookmarked,
                incognito = state.incognito,
                onBackClick = onBackClick,
                onBookmarkClick = { onChromeAction(ReaderAction.ToggleBookmark) },
            )
        }

        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = bottomChromeEnter,
            exit = bottomChromeExit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomChrome(
                pageIndex = state.pageIndex,
                pageCount = state.pageCount,
                direction = state.direction,
                pageFit = state.pageFit,
                sliderInteraction = sliderInteraction,
                onAction = onChromeAction,
                onScrubChange = { chromeScrubHeld = it },
            )
        }

        if (state.settingsOpen) {
            ReaderSettingsSheet(
                direction = state.direction,
                pageFit = state.pageFit,
                cropMargins = state.cropMargins,
                volumeKeys = state.volumeKeys,
                volumeKeysInverted = state.volumeKeysInverted,
                incognito = state.incognito,
                displayFilter = state.displayFilter,
                hasFilterOverride = state.hasFilterOverride,
                keepScreenOn = state.keepScreenOn,
                showTapZones = state.showTapZones,
                showPageCounter = state.showPageCounter,
                swipeToTurn = state.swipeToTurn,
                dualPageSplit = state.dualPageSplit,
                dualPageInvert = state.dualPageInvert,
                onAction = onAction,
            )
        }

        if (state.overviewOpen) {
            ReaderOverviewSheet(
                comicId = state.comicId,
                currentPage = state.currentPage,
                expandedCount = state.pageCount,
                currentArchiveIndex = state.currentArchiveIndex,
                archivePageCount = state.archivePageCount,
                expandedForArchive = state.expandedForArchive,
                cropMargins = state.cropMargins,
                onAction = onAction,
            )
        }

        // Mini page counter while the chrome is away:
        // the one orientation cue readers keep when controls hide.
        AnimatedVisibility(
            visible = !state.chromeVisible && !state.settingsOpen && !state.overviewOpen && state.showPageCounter,
            enter = MoriMotion.enter(MoriEnterKind.FADE),
            exit = MoriMotion.exit(MoriEnterKind.FADE),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MoriScrimPill(
                text = stringResource(R.string.reader_counter, state.currentPage, state.pageCount),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .padding(
                        bottom = 24.dp +
                            WindowInsets.safeDrawing.asPaddingValues()
                                .calculateBottomPadding(),
                    )
                    .testTag(ReaderTestTags.PageCounter),
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    subtitle: String,
    bookmarked: Boolean,
    incognito: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Safe drawing (bars + cutout), not status bars alone: landscape cutouts
    // and gesture/3-button nav insets would otherwise sit chrome under glass.
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                        Color.Transparent,
                    ),
                ),
            )
            .swallowTaps()
            .padding(top = safeDrawing.calculateTopPadding())
            .testTag(ReaderTestTags.TopBar),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    start = maxOf(
                        4.dp,
                        safeDrawing.calculateStartPadding(layoutDirection),
                    ),
                    end = maxOf(4.dp, safeDrawing.calculateEndPadding(layoutDirection)),
                    top = 8.dp,
                    bottom = 8.dp,
                ),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = MoriIcons.Back,
                    contentDescription = stringResource(R.string.reader_back),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { stringResource(R.string.reader_untitled) },
                    style = MoriEmphasized.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val bookmarkLabel = if (bookmarked) {
                stringResource(R.string.reader_bookmark_remove)
            } else {
                stringResource(R.string.reader_bookmark)
            }
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .testTag(ReaderTestTags.Bookmark)
                    .semantics {
                        onClick(label = bookmarkLabel, action = null)
                        stateDescription = bookmarkLabel
                    },
            ) {
                Icon(
                    imageVector = if (bookmarked) MoriIcons.Bookmark else MoriIcons.BookmarkBorder,
                    contentDescription = bookmarkLabel,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            // Incognito indicator: decorative badge, announced once for
            // TalkBack so private reading is never ambiguous.
            if (incognito) {
                Icon(
                    imageVector = MoriIcons.Incognito,
                    contentDescription = stringResource(R.string.reader_incognito_on),
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .testTag(ReaderTestTags.IncognitoBadge),
                )
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun ReaderBottomChrome(
    pageIndex: Int,
    pageCount: Int,
    direction: ReadingDirection,
    pageFit: PageFit,
    sliderInteraction: MutableInteractionSource,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
    onScrubChange: (Boolean) -> Unit = {},
) {
    // The whole row mirrors in RTL so the forward control stays on the leading side,
    // matching the pager's own reversal.
    val rowDirection = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    // In RTL the leading control advances; icons follow the visual direction.
    // Descriptions name the action, not the side, so TalkBack stays truthful.
    val nextLabel = stringResource(R.string.reader_next_page)
    val prevLabel = stringResource(R.string.reader_previous_page)
    val leadingAction = remember(direction, nextLabel, prevLabel) {
        if (direction == ReadingDirection.RIGHT_TO_LEFT) {
            Triple(ReaderAction.NextPage, MoriIcons.SkipNext, nextLabel)
        } else {
            Triple(ReaderAction.PrevPage, MoriIcons.SkipPrevious, prevLabel)
        }
    }
    val trailingAction = remember(direction, nextLabel, prevLabel) {
        if (direction == ReadingDirection.RIGHT_TO_LEFT) {
            Triple(ReaderAction.PrevPage, MoriIcons.SkipPrevious, prevLabel)
        } else {
            Triple(ReaderAction.NextPage, MoriIcons.SkipNext, nextLabel)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                    ),
                ),
            )
            .swallowTaps()
            .padding(horizontal = 16.dp)
            .padding(
                bottom = 16.dp +
                    WindowInsets.safeDrawing.asPaddingValues()
                        .calculateBottomPadding(),
            ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides rowDirection) {
            // Single-page books have nowhere to turn: the whole nav row
            // (buttons + slider) hides instead of rendering disabled.
            if (pageCount > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                    .fillMaxWidth(),
            ) {
                FilledIconButton(
                    onClick = { onAction(leadingAction.first) },
                    enabled = isNavigationEnabled(leadingAction.first, pageIndex, pageCount),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .size(CHROME_CONTROL_SIZE)
                        .testTag(ReaderTestTags.Prev)
                        .semantics {
                            onClick(label = leadingAction.third, action = null)
                        },
                ) {
                    Icon(
                        imageVector = leadingAction.second,
                        contentDescription = leadingAction.third,
                    )
                }

                // Scrub preview: the thumb and the number follow the finger
                // locally and commit once on release, so a long scrub fires
                // one seek (one glide, one debounced save) instead of a seek
                // per drag tick. Hoisted above the pill so the number tracks
                // live: the pill doubles as the slider's value indicator.
                // Reset per book length so a stale held index can't leak across books.
                var scrub by remember(pageCount) { mutableStateOf<Int?>(null) }
                LaunchedEffect(scrub) { onScrubChange(scrub != null) }
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    ) {
                        // Transparent widest-length text behind the current number keeps
                        // the slider from shifting as digit counts change. The
                        // number tracks the scrub live: the pill doubles as the
                        // slider's value indicator.
                        Box(contentAlignment = Alignment.CenterEnd) {
                            Text(
                                text = ((scrub ?: pageIndex) + 1).toString(),
                                    style = MoriEmphasized.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = pageCount.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Transparent,
                                    modifier = Modifier.semantics { invisibleToUser() },
                                )
                            }
                            val scrubDescription = stringResource(
                                R.string.reader_pager_description,
                                (scrub ?: pageIndex) + 1,
                                pageCount,
                            )
                            // Slider stays LTR even inside the mirrored RTL row so
                            // scrub direction never flips with reading direction.
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Slider(
                                    value = (scrub ?: pageIndex).toFloat(),
                                    onValueChange = { scrub = it.roundToInt() },
                                    onValueChangeFinished = {
                                        scrub?.let { onAction(ReaderAction.SeekPage(it)) }
                                        scrub = null
                                    },
                                    valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                                    // Continuous: discrete steps quantize long books
                                    // into jumps; rounding lands the nearest page.
                                    steps = 0,
                                    interactionSource = sliderInteraction,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(ReaderTestTags.Slider)
                                        .semantics {
                                            contentDescription = scrubDescription
                                        },
                                )
                            }
                            Text(
                                text = pageCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { invisibleToUser() },
                            )
                        }
                    }

                FilledIconButton(
                    onClick = { onAction(trailingAction.first) },
                    enabled = isNavigationEnabled(trailingAction.first, pageIndex, pageCount),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .size(CHROME_CONTROL_SIZE)
                        .testTag(ReaderTestTags.Next)
                        .semantics {
                            onClick(label = trailingAction.third, action = null)
                        },
                ) {
                    Icon(
                        imageVector = trailingAction.second,
                        contentDescription = trailingAction.third,
                    )
                }
            }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                .height(CHROME_CONTROL_SIZE),
        ) {
            val directionLabel = stringResource(R.string.reader_reading_direction)
            val directionState = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
                stringResource(R.string.reader_direction_rtl)
            } else {
                stringResource(R.string.reader_direction_ltr)
            }
            IconButton(
                onClick = {
                    val next = when (direction) {
                        ReadingDirection.LEFT_TO_RIGHT -> ReadingDirection.RIGHT_TO_LEFT
                        ReadingDirection.RIGHT_TO_LEFT -> ReadingDirection.LEFT_TO_RIGHT
                    }
                    onAction(ReaderAction.SetDirection(next))
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ReaderTestTags.DirectionButton)
                    .semantics {
                        onClick(label = directionLabel, action = null)
                        stateDescription = directionState
                    },
            ) {
                Icon(
                    imageVector = MoriIcons.ScreenRotation,
                    contentDescription = directionLabel,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            val fitLabel = stringResource(R.string.reader_page_fit)
            val fitState = when (pageFit) {
                PageFit.WIDTH -> stringResource(R.string.reader_fit_width)
                PageFit.HEIGHT -> stringResource(R.string.reader_fit_height)
                PageFit.ORIGINAL -> stringResource(R.string.reader_fit_original)
            }
            IconButton(
                onClick = {
                    val next = when (pageFit) {
                        PageFit.WIDTH -> PageFit.HEIGHT
                        PageFit.HEIGHT -> PageFit.ORIGINAL
                        PageFit.ORIGINAL -> PageFit.WIDTH
                    }
                    onAction(ReaderAction.SetPageFit(next))
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ReaderTestTags.FitButton)
                    .semantics {
                        onClick(label = fitLabel, action = null)
                        stateDescription = fitState
                    },
            ) {
                Icon(
                    imageVector = MoriIcons.FitScreen,
                    contentDescription = fitLabel,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            val cropLabel = stringResource(R.string.reader_crop_margins)
            IconButton(
                onClick = { onAction(ReaderAction.ToggleCrop) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ReaderTestTags.CropButton)
                    .semantics {
                        onClick(label = cropLabel, action = null)
                    },
            ) {
                Icon(
                    imageVector = MoriIcons.Crop,
                    contentDescription = cropLabel,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.OpenOverview) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ReaderTestTags.OverviewButton),
            ) {
                Icon(
                    imageVector = MoriIcons.GridView,
                    contentDescription = stringResource(R.string.reader_overview_button),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.OpenSettings) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(ReaderTestTags.SettingsButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Settings,
                    contentDescription = stringResource(R.string.reader_settings),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ReaderScreenPreview() {
    MoriTheme {
        ReaderScreen(
            uiState = ReaderUiState.Ready(
                comicId = "batman",
                title = "Batman",
                subtitle = "Court of Owls (2012) (digital) (Minutemen-PhD)",
                bookmarked = false,
                pageIndex = 12,
                pageCount = 173,
                chromeVisible = true,
                direction = ReadingDirection.LEFT_TO_RIGHT,
                pageFit = PageFit.WIDTH,
                cropMargins = false,
                settingsOpen = false,
                overviewOpen = false,
                volumeKeys = false,
                volumeKeysInverted = false,
                keepScreenOn = true,
                showTapZones = false,
                showPageCounter = true,
                swipeToTurn = true,
            ),
            onAction = {},
            onBackClick = {},
        )
    }
}

private const val CHROME_AUTO_HIDE_MS = 3000L

/** Delay before hiding system bars after chrome settles (show stays instant). */
private const val CHROME_SETTLE_DELAY_MS = 120L

/** Predictive-back shrink at full gesture progress (subtle bed pull). */
private const val BACK_PREVIEW_SHRINK = 0.05f

/** Predictive-back dim at full gesture progress. */
private const val BACK_PREVIEW_FADE = 0.15f

/** Touch target for chrome controls (M3 minimum, down from 56dp). */
private val CHROME_CONTROL_SIZE = 48.dp

/** Content width cap on expanded windows (M3 readability guidance). */
private val EXPANDED_CONTENT_MAX_WIDTH = 840.dp

/** Neighbor pages shrink by this fraction at full offset (carousel feel). */
private const val PAGE_SHRINK = 0.08f

/**
 * Whether a navigation action can move anywhere from [pageIndex].
 *
 * Buttons render disabled at the ends instead of clamping silently, so position is
 * always visible.
 */
private fun isNavigationEnabled(action: ReaderAction, pageIndex: Int, pageCount: Int): Boolean =
    when (action) {
        ReaderAction.PrevPage -> pageIndex > 0
        ReaderAction.NextPage -> pageIndex < pageCount - 1
        else -> true
    }

/**
 * Consumes taps on chrome containers so they never fall through to the page zones
 * beneath (which would turn pages when the user meant to scrub or open settings).
 */
private fun Modifier.swallowTaps(): Modifier = clickable(
    interactionSource = null,
    indication = null,
    onClick = {},
)
