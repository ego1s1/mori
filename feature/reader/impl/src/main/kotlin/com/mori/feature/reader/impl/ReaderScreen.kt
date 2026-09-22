package com.mori.feature.reader.impl

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
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
    // Layout generation for the page slots below: any rebuild of the viewer
    // list (split/direction/invert/scan) remounts pages at fit. Computed
    // once per list instance so slot lookup stays O(1).
    val viewerLayoutKey = remember(state.viewerPages) { state.viewerPages.hashCode() }

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
        if (pagerState.currentPage != target) {
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
    // Zoom/pan ownership (Mihon edge-handoff): while a page reports zoomed
    // or pinching, swipes belong to the page — the pager stands down so it
    // can never steal the gesture, and turns happen only through the
    // explicit edge dispatch below. A settled page is always at fit, so any
    // pager move resets the gate.
    var zoomed by remember { mutableStateOf(false) }
    var pinching by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            zoomed = false
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

    // Back exits through the NavHost: Navigation Compose scrubs the pop
    // transitions with the system gesture, so the library shows through for
    // real. No custom handler here — consuming the press would block that and
    // the settings sheet dismisses itself first via its own back handling.

    // Auto-hide chrome after a moment of stillness, but never mid-scrub.
    if (state.chromeVisible && !state.settingsOpen && !state.overviewOpen && !scrubbing) {
        LaunchedEffect(state.chromeVisible, state.pageIndex) {
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
    DisposableEffect(context, state.chromeVisible) {
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (state.chromeVisible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
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
            val containerWidthPx = with(LocalDensity.current) {
                maxWidth.toPx()
            }.coerceAtLeast(1f)
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
                    userScrollEnabled = state.swipeToTurn && !zoomed && !pinching,
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
                    val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue
                    // Dual-page split: the pager walks expanded positions, each
                    // resolving to an archive page plus the half to decode.
                    val viewerPage = state.viewerPages.getOrNull(page)
                        ?: ReaderViewerPage(page, PageHalf.FULL)
                    // Remount on list rebuilds (split/direction/invert/scan):
                    // zoom and pan belong to a layout, not a slot — the page
                    // remembers zoom by archive identity, so carrying it
                    // across a rebuild would strand the zoomed gate on a page
                    // it no longer describes and freeze swipe-turns. Fresh
                    // pages start at fit with the gate released. The key is a
                    // content hash, not the list: equality is O(pages) once
                    // per rebuild, O(1) per composition afterwards.
                    key(viewerLayoutKey) {
                        ZoomablePage(
                        comicId = state.comicId,
                        pageIndex = viewerPage.archiveIndex,
                        pageNumber = viewerPage.archiveIndex + 1,
                        pageFit = state.pageFit,
                        direction = state.direction,
                        cropMargins = state.cropMargins,
                        half = viewerPage.half,
                        onZoomedChange = { zoomed = it },
                        onPinchingChange = { pinching = it },
                        onEdgeTurn = { forward ->
                            // swipeToTurn off means swipes never turn — taps own that.
                            if (state.swipeToTurn) {
                                onAction(if (forward) ReaderAction.NextPage else ReaderAction.PrevPage)
                            }
                        },
                        modifier = Modifier.graphicsLayer {
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

        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = topChromeEnter,
            exit = topChromeExit,
        ) {
            ReaderTopBar(
                title = state.title,
                subtitle = state.subtitle,
                bookmarked = state.bookmarked,
                onBackClick = onBackClick,
                onBookmarkClick = { onAction(ReaderAction.ToggleBookmark) },
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
                onAction = onAction,
            )
        }

        if (state.settingsOpen) {
            ReaderSettingsSheet(
                direction = state.direction,
                pageFit = state.pageFit,
                cropMargins = state.cropMargins,
                volumeKeys = state.volumeKeys,
                volumeKeysInverted = state.volumeKeysInverted,
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

        // Mini page counter while the chrome is away (Mihon's show-page-number):
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
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { stringResource(R.string.reader_untitled) },
                    style = MoriEmphasized.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.testTag(ReaderTestTags.Bookmark),
            ) {
                Icon(
                    imageVector = if (bookmarked) MoriIcons.Bookmark else MoriIcons.BookmarkBorder,
                    contentDescription = if (bookmarked) stringResource(R.string.reader_bookmark_remove) else stringResource(R.string.reader_bookmark),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomChrome(
    pageIndex: Int,
    pageCount: Int,
    direction: ReadingDirection,
    pageFit: PageFit,
    sliderInteraction: MutableInteractionSource,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
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
    val leadingAction = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        Triple(ReaderAction.NextPage, MoriIcons.SkipNext, nextLabel)
    } else {
        Triple(ReaderAction.PrevPage, MoriIcons.SkipPrevious, prevLabel)
    }
    val trailingAction = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        Triple(ReaderAction.PrevPage, MoriIcons.SkipPrevious, prevLabel)
    } else {
        Triple(ReaderAction.NextPage, MoriIcons.SkipNext, nextLabel)
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
                        .testTag(ReaderTestTags.Prev),
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
                var scrub by remember { mutableStateOf<Int?>(null) }
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
                                )
                            }
                            val scrubDescription = stringResource(
                                R.string.reader_pager_description,
                                (scrub ?: pageIndex) + 1,
                                pageCount,
                            )
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
                            Text(
                                text = pageCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .testTag(ReaderTestTags.Next),
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
            IconButton(
                onClick = {
                    val next = when (direction) {
                        ReadingDirection.LEFT_TO_RIGHT -> ReadingDirection.RIGHT_TO_LEFT
                        ReadingDirection.RIGHT_TO_LEFT -> ReadingDirection.LEFT_TO_RIGHT
                    }
                    onAction(ReaderAction.SetDirection(next))
                },
                modifier = Modifier.testTag(ReaderTestTags.DirectionButton),
            ) {
                Icon(
                    imageVector = MoriIcons.ScreenRotation,
                    contentDescription = stringResource(R.string.reader_reading_direction),
                    tint = Color.White,
                )
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
                modifier = Modifier.testTag(ReaderTestTags.FitButton),
            ) {
                Icon(
                    imageVector = MoriIcons.FitScreen,
                    contentDescription = stringResource(R.string.reader_page_fit),
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.ToggleCrop) },
                modifier = Modifier.testTag(ReaderTestTags.CropButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Crop,
                    contentDescription = stringResource(R.string.reader_crop_margins),
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.OpenOverview) },
                modifier = Modifier.testTag(ReaderTestTags.OverviewButton),
            ) {
                Icon(
                    imageVector = MoriIcons.GridView,
                    contentDescription = stringResource(R.string.reader_overview_button),
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.OpenSettings) },
                modifier = Modifier.testTag(ReaderTestTags.SettingsButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Settings,
                    contentDescription = stringResource(R.string.reader_settings),
                    tint = Color.White,
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
