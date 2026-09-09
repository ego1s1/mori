package com.mori.feature.reader.impl

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriTheme
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.ThemePreviews
import com.mori.core.designsystem.MoriMotion
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
internal fun ReaderRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
        color = MaterialTheme.colorScheme.scrim,
    ) {
        when (uiState) {
            ReaderUiState.Loading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }

            is ReaderUiState.Error -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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

    // ViewModel -> pager (buttons, slider, external seeks).
    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) {
            pagerState.animateScrollToPage(state.pageIndex)
        }
    }
    // Pager -> ViewModel (swipes).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onAction(ReaderAction.PageChanged(page))
        }
    }

    val rtl = state.direction == ReadingDirection.RIGHT_TO_LEFT
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sliderInteraction = remember { MutableInteractionSource() }
    val scrubbing by sliderInteraction.collectIsDraggedAsState()

    // Predictive back: the page shrinks and fades with the gesture, and the app
    // only leaves the reader when the gesture commits. Cancelled gestures snap
    // back. Disabled over the settings sheet so back dismisses the sheet first.
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = !state.settingsOpen) { progress ->
        try {
            progress.collect { event -> backProgress = event.progress }
            backProgress = 0f
            onBackClick()
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    // Auto-hide chrome after a moment of stillness, but never mid-scrub.
    if (state.chromeVisible && !state.settingsOpen && !scrubbing) {
        LaunchedEffect(state.chromeVisible, state.pageIndex) {
            delay(CHROME_AUTO_HIDE_MS)
            onAction(ReaderAction.ToggleChrome)
        }
    }
    // Tactile ticks while scrubbing through pages.
    LaunchedEffect(state.pageIndex) {
        if (scrubbing) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Chrome entrances follow the motion setting: expressive springs, calm fades.
    val (topChromeEnter, topChromeExit) = chromeTransitions(top = true)
    val (bottomChromeEnter, bottomChromeExit) = chromeTransitions(top = false)

    if (state.keepScreenOn) {
        DisposableEffect(context) {
            val window = (context as? android.app.Activity)?.window
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = predictiveBackScale(backProgress)
                scaleX = scale
                scaleY = scale
                alpha = 1f - PREDICTIVE_BACK_FADE * backProgress.coerceIn(0f, 1f)
            }
            .onPreviewKeyEvent { event ->
                val action = volumeKeyAction(event.key, event.type, state.volumeKeys)
                if (action != null) {
                    onAction(action)
                    true
                } else {
                    false
                }
            },
    ) {
        // Constrain the page well on expanded windows (M3 guidance caps gallery
        // content around 840dp); phones stay full-bleed.
        androidx.compose.foundation.layout.BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            val pageWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
            HorizontalPager(
                state = pagerState,
                reverseLayout = rtl,
                beyondViewportPageCount = 1,
                userScrollEnabled = true,
                modifier = Modifier
                    .width(pageWidth)
                    .fillMaxHeight()
                    .testTag(ReaderTestTags.Pager),
            ) { page ->
            // Expressive page transform: neighbors shrink and fade like a carousel,
            // giving swipe momentum a physical feel.
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue
            ZoomablePage(
                comicId = state.comicId,
                pageIndex = page,
                pageNumber = page + 1,
                pageFit = state.pageFit,
                direction = state.direction,
                modifier = Modifier.graphicsLayer {
                    val scale = 1f - (pageOffset * PAGE_SHRINK).coerceIn(0f, PAGE_SHRINK)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (pageOffset * PAGE_FADE).coerceIn(0f, PAGE_FADE)
                },
                onZoneTap = { zone ->
                    when (zone) {
                        ReaderZone.PREV -> onAction(ReaderAction.PrevPage)
                        ReaderZone.NEXT -> onAction(ReaderAction.NextPage)
                        ReaderZone.MENU -> onAction(ReaderAction.ToggleChrome)
                    }
                },
            )
            }
        }

        if (state.showTapZones) {
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
                keepScreenOn = state.keepScreenOn,
                showTapZones = state.showTapZones,
                onAction = onAction,
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
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Transparent,
                    ),
                ),
            )
            .swallowTaps()
            .padding(top = statusBarPadding.calculateTopPadding())
            .testTag(ReaderTestTags.TopBar),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = MoriIcons.Back,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Untitled comic" },
                    style = MaterialTheme.typography.headlineSmall,
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
                    contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark",
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
    val leadingAction = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        ReaderAction.NextPage to MoriIcons.SkipNext
    } else {
        ReaderAction.PrevPage to MoriIcons.SkipPrevious
    }
    val trailingAction = if (direction == ReadingDirection.RIGHT_TO_LEFT) {
        ReaderAction.PrevPage to MoriIcons.SkipPrevious
    } else {
        ReaderAction.NextPage to MoriIcons.SkipNext
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.72f),
                    ),
                ),
            )
            .swallowTaps()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides rowDirection) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledIconButton(
                    onClick = { onAction(leadingAction.first) },
                    enabled = isNavigationEnabled(leadingAction.first, pageIndex, pageCount),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag(ReaderTestTags.Prev),
                ) {
                    Icon(
                        imageVector = leadingAction.second,
                        contentDescription = "Previous page",
                    )
                }

                if (pageCount > 1) {
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
                            // the slider from shifting as digit counts change.
                            Box(contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = (pageIndex + 1).toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = pageCount.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Transparent,
                                )
                            }
                            Slider(
                                value = pageIndex.toFloat(),
                                onValueChange = { onAction(ReaderAction.SeekPage(it.toInt())) },
                                valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                                steps = (pageCount - 2).coerceAtLeast(0),
                                interactionSource = sliderInteraction,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(ReaderTestTags.Slider)
                                    .semantics {
                                        contentDescription = "Page ${pageIndex + 1} of $pageCount"
                                    },
                            )
                            Text(
                                text = pageCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                FilledIconButton(
                    onClick = { onAction(trailingAction.first) },
                    enabled = isNavigationEnabled(trailingAction.first, pageIndex, pageCount),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag(ReaderTestTags.Next),
                ) {
                    Icon(
                        imageVector = trailingAction.second,
                        contentDescription = "Next page",
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
                    contentDescription = "Reading direction",
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
                    contentDescription = "Page fit",
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.ToggleCrop) },
                modifier = Modifier.testTag(ReaderTestTags.CropButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Crop,
                    contentDescription = "Crop margins",
                    tint = Color.White,
                )
            }
            IconButton(
                onClick = { onAction(ReaderAction.OpenSettings) },
                modifier = Modifier.testTag(ReaderTestTags.SettingsButton),
            ) {
                Icon(
                    imageVector = MoriIcons.Settings,
                    contentDescription = "Reader settings",
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
                volumeKeys = false,
                keepScreenOn = true,
                showTapZones = false,
            ),
            onAction = {},
            onBackClick = {},
        )
    }
}

private const val CHROME_AUTO_HIDE_MS = 3000L

/** Page shrink at a fully-committed predictive back gesture. */
private const val PREDICTIVE_BACK_SHRINK = 0.08f

/** Page fade at a fully-committed predictive back gesture. */
private const val PREDICTIVE_BACK_FADE = 0.25f

/**
 * Chrome enter/exit pair for the top (`top = true`) or bottom bar.
 *
 * Expressive motion slides on the chrome spring; calm motion (or system reduced
 * motion) fades quietly. Read from [LocalExpressiveMotionEnabled] so the settings
 * toggle takes effect without threading flags through state.
 */
@Composable
private fun chromeTransitions(top: Boolean): Pair<EnterTransition, ExitTransition> {
    val expressive = LocalExpressiveMotionEnabled.current
    return remember(expressive, top) {
        if (expressive) {
            val offset = if (top) -1 else 1
            val enter = fadeIn(animationSpec = MoriMotion.chromeSpring()) +
                slideInVertically(animationSpec = MoriMotion.chromeSpring()) { offset * it / 2 }
            val exit = fadeOut(animationSpec = MoriMotion.chromeSpring()) +
                slideOutVertically(animationSpec = MoriMotion.chromeSpring()) { offset * it / 2 }
            enter to exit
        } else {
            fadeIn(animationSpec = MoriMotion.calmFade()) to
                fadeOut(animationSpec = MoriMotion.calmFade())
        }
    }
}

/**
 * Page scale for a predictive back [progress] (`0f` at rest, `1f` committed).
 *
 * The reader dips slightly as the gesture drives home, mirroring the system
 * back preview without ever leaving the page on a cancelled gesture. Pure for
 * testability.
 */
internal fun predictiveBackScale(progress: Float): Float =
    1f - PREDICTIVE_BACK_SHRINK * progress.coerceIn(0f, 1f)
/** Content width cap on expanded windows (M3 readability guidance). */
private val EXPANDED_CONTENT_MAX_WIDTH = 840.dp

/** Neighbor pages shrink by this fraction at full offset (carousel feel). */
private const val PAGE_SHRINK = 0.08f

/** Neighbor pages fade by this fraction at full offset. */
private const val PAGE_FADE = 0.4f

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
