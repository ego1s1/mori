package com.mori.feature.reader.impl

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.LocalAppFonts
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriComicErrorCard
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriEnterKind
import com.mori.core.designsystem.MoriErrorCard
import com.mori.core.designsystem.MoriHaptic
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriLoading
import com.mori.core.designsystem.MoriMotion
import com.mori.core.designsystem.MoriScrimPill
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.enter
import com.mori.core.designsystem.exit
import com.mori.core.designsystem.rememberMoriHaptics
import com.mori.core.model.ComicError
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection
import com.mori.feature.reader.api.ReaderKeyInterceptor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
    // Tracks the in-flight programmatic destination so rapid chains retarget
    // instead of dropping: the skip below only applies when the pager is
    // already converging on this exact target.
    var animatingTo by remember { mutableIntStateOf(-1) }
    LaunchedEffect(state.pageIndex, state.pageCount, state.turnAnimated, pagerExpressive) {
        val target = state.pageIndex.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
        if (target == pagerState.currentPage && pagerState.currentPageOffsetFraction == 0f) {
            animatingTo = target
            return@LaunchedEffect
        }
        // Skip while settling toward this target: a swipe-driven offset is
        // already converging on it; retargeting would fight the gesture.
        // A *newer* target falls through and restarts from the live offset.
        if (target == animatingTo && pagerState.currentPageOffsetFraction != 0f) {
            return@LaunchedEffect
        }
        animatingTo = target
        if (pagerExpressive && state.turnAnimated) {
            pagerState.animateScrollToPage(
                target,
                animationSpec = MoriMotion.pageTurnSpec(),
            )
        } else {
            pagerState.scrollToPage(target)
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
    val sliderInteraction = remember { MutableInteractionSource() }
    val scrubbing by sliderInteraction.collectIsDraggedAsState()
    val contentScope = rememberCoroutineScope()
    // Crossfade level for overview seeks; 1 at rest.
    val pagerFade = remember { Animatable(1f) }
    // Detector epoch for the container taps: a direction flip bumps it so a
    // double-tap hold parked across the flip drops instead of dispatching
    // with the old zone. Starts at 1 — the launch effect below runs on first
    // composition too, and 0 must never read as a valid stamp.
    val tapEpoch = remember { mutableIntStateOf(1) }
    LaunchedEffect(state.direction) { tapEpoch.intValue++ }

    // Back exits through the NavHost: Navigation Compose scrubs the pop
    // transitions with the system gesture, so Main shows through for real.
    // No custom preview handler here — consuming the press would block that
    // scrub and hide Main behind a shrinking bed. Sheets own the back press
    // while open via their own back handling.

    // Chrome interaction epoch: bumped by chrome control callbacks
    // (direction/fit/crop/settings/overview/bookmark/nav) so the auto-hide timer
    // below restarts on any chrome interaction, not just page turns.
    var chromeInteractionEpoch by remember { mutableIntStateOf(0) }
    // Mirrors ReaderBottomChrome's local `scrub != null` (slider held but drag
    // detection lagging); updated via onScrubChange below.
    var chromeScrubHeld by remember { mutableStateOf(false) }
    val onChromeAction: (ReaderAction) -> Unit = { action ->
        when (action) {
            ReaderAction.HideChrome, ReaderAction.ToggleChrome -> Unit
            else -> chromeInteractionEpoch++
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
            // dispatches exactly once.
            val density = LocalDensity.current
            val containerWidthPx = remember(density, maxWidth) {
                with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            }
            val viewportWidth = rememberUpdatedState(containerWidthPx)
            val pageWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zoneTaps(
                        viewportWidth = viewportWidth,
                        direction = state.direction,
                        scope = contentScope,
                        epoch = tapEpoch,
                        onZoneTap = handleZone,
                        onZoom = { _, _ -> handleZone(ReaderZone.MENU) },
                        consumeUp = false,
                        navMode = state.navMode,
                        invertMode = state.invertTaps,
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
                        .graphicsLayer {
                            // Overview-seek crossfade level; swipes bypass it
                            alpha = pagerFade.value
                        }
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
                    val viewerPage = pages.getOrNull(page)
                        ?: ReaderViewerPage(page, PageHalf.FULL)
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
                            navMode = state.navMode,
                            invertMode = state.invertTaps,
                            onPinchingChange = { pinching = it },
                            onEdgeTurn = { forward ->
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
            TapZoneOverlay(
                direction = state.direction,
                navMode = state.navMode,
                invertMode = state.invertTaps,
            )
        }

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
                cropMargins = state.cropMargins,
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
                navMode = state.navMode,
                invertTaps = state.invertTaps,
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
                onAction = { action ->
                    if (action is ReaderAction.SeekPage && pagerExpressive) {
                        contentScope.launch {
                            pagerFade.animateTo(0f, MoriMotion.defaultEffectsSpec())
                            onAction(action.copy(animated = false))
                            pagerFade.animateTo(1f, MoriMotion.defaultEffectsSpec())
                        }
                    } else {
                        onAction(action)
                    }
                },
            )
        }

        // Mini page counter while the chrome is away:
        // Dynamic Floating Scrim Pill that stays legible over bright comic art
        AnimatedVisibility(
            visible = !state.chromeVisible && !state.settingsOpen && !state.overviewOpen && state.showPageCounter,
            enter = MoriMotion.enter(MoriEnterKind.FADE),
            exit = MoriMotion.exit(MoriEnterKind.FADE),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MoriScrimPill(
                text = stringResource(R.string.reader_counter, state.currentPage, state.pageCount),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                textStyle = MoriEmphasized.labelLarge,
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
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val haptics = rememberMoriHaptics()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .swallowTaps()
            .testTag(ReaderTestTags.TopBar),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .padding(
                    start = maxOf(
                        12.dp,
                        safeDrawing.calculateStartPadding(layoutDirection),
                    ),
                    end = maxOf(12.dp, safeDrawing.calculateEndPadding(layoutDirection)),
                    top = safeDrawing.calculateTopPadding() + 6.dp,
                    bottom = 10.dp,
                ),
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics(MoriHaptic.Select)
                    onBackClick()
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = MoriIcons.Back,
                    contentDescription = stringResource(R.string.reader_back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { stringResource(R.string.reader_untitled) },
                    style = MoriEmphasized.headlineSmall.copy(
                        fontFamily = LocalAppFonts.current.topBarTitle,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MoriEmphasized.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            FilledTonalIconButton(
                onClick = {
                    haptics(MoriHaptic.Select)
                    onBookmarkClick()
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (bookmarked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (bookmarked) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
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
                )
            }
            if (incognito) {
                Icon(
                    imageVector = MoriIcons.Incognito,
                    contentDescription = stringResource(R.string.reader_incognito_on),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 4.dp)
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
    cropMargins: Boolean,
    sliderInteraction: MutableInteractionSource,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
    onScrubChange: (Boolean) -> Unit = {},
) {
    val haptics = rememberMoriHaptics()
    val rowDirection = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
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

    var scrub by remember(pageCount) { mutableStateOf<Int?>(null) }
    LaunchedEffect(scrub) { onScrubChange(scrub != null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .swallowTaps()
            .padding(horizontal = 16.dp)
            .padding(
                bottom = 16.dp +
                        WindowInsets.safeDrawing.asPaddingValues()
                            .calculateBottomPadding(),
            ),
    ) {
        // Floating live tooltip bubble while scrubbing
        AnimatedVisibility(
            visible = scrub != null,
            enter = fadeIn(MoriMotion.defaultEffectsSpec()) + scaleIn(
                MoriMotion.defaultSpatialSpec(),
                initialScale = 0.85f
            ),
            exit = fadeOut(MoriMotion.defaultEffectsSpec()) + scaleOut(
                MoriMotion.defaultSpatialSpec(),
                targetScale = 0.85f
            ),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp,
            ) {
                Text(
                    text = stringResource(R.string.reader_counter, (scrub ?: pageIndex) + 1, pageCount),
                    style = MoriEmphasized.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        // Unified Floating Expressive Pill Bar Container
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            ),
            modifier = Modifier
                .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Scrubber Row: Steppers + Continuous Slider with Live Page Indicator
                if (pageCount > 1) {
                    CompositionLocalProvider(LocalLayoutDirection provides rowDirection) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    haptics(MoriHaptic.Select)
                                    onAction(leadingAction.first)
                                },
                                enabled = isNavigationEnabled(leadingAction.first, pageIndex, pageCount),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
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

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                ) {
                                    // Live current page number with ghost placeholder for layout stability
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        Text(
                                            text = ((scrub ?: pageIndex) + 1).toString(),
                                            style = MoriEmphasized.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = pageCount.toString(),
                                            style = MoriEmphasized.titleMedium,
                                            color = Color.Transparent,
                                            modifier = Modifier.semantics { hideFromAccessibility() },
                                        )
                                    }
                                    val scrubDescription = stringResource(
                                        R.string.reader_pager_description,
                                        (scrub ?: pageIndex) + 1,
                                        pageCount,
                                    )
                                    // Slider stays LTR even inside mirrored RTL row
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Slider(
                                            value = (scrub ?: pageIndex).toFloat(),
                                            onValueChange = {
                                                val nextScrub = it.roundToInt()
                                                if (nextScrub != scrub) {
                                                    haptics(MoriHaptic.FrequentTick)
                                                }
                                                scrub = nextScrub
                                            },
                                            onValueChangeFinished = {
                                                haptics(MoriHaptic.Select)
                                                scrub?.let { onAction(ReaderAction.SeekPage(it)) }
                                                scrub = null
                                            },
                                            valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    haptics(MoriHaptic.Select)
                                    onAction(trailingAction.first)
                                },
                                enabled = isNavigationEnabled(trailingAction.first, pageIndex, pageCount),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
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

                // Quick Action Controls Row: Direction, Fit, Crop, Overview, Settings
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
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
                            haptics(MoriHaptic.Select)
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            haptics(MoriHaptic.Select)
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    val cropLabel = stringResource(R.string.reader_crop_margins)
                    val cropState = if (cropMargins) {
                        stringResource(R.string.reader_crop_subtitle)
                    } else {
                        stringResource(R.string.reader_crop_title)
                    }
                    IconButton(
                        onClick = {
                            haptics(MoriHaptic.Select)
                            onAction(ReaderAction.ToggleCrop)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(ReaderTestTags.CropButton)
                            .semantics {
                                onClick(label = cropLabel, action = null)
                                stateDescription = cropState
                            },
                    ) {
                        Icon(
                            imageVector = MoriIcons.Crop,
                            contentDescription = cropLabel,
                            tint = if (cropMargins) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics(MoriHaptic.Select)
                            onAction(ReaderAction.OpenOverview)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(ReaderTestTags.OverviewButton),
                    ) {
                        Icon(
                            imageVector = MoriIcons.GridView,
                            contentDescription = stringResource(R.string.reader_overview_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics(MoriHaptic.Select)
                            onAction(ReaderAction.OpenSettings)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(ReaderTestTags.SettingsButton),
                    ) {
                        Icon(
                            imageVector = MoriIcons.Settings,
                            contentDescription = stringResource(R.string.reader_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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

private const val CHROME_AUTO_HIDE_MS = 5000L

/** Delay before hiding system bars after chrome settles (show stays instant). */
private const val CHROME_SETTLE_DELAY_MS = 120L

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
