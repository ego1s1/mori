package com.mori.feature.reader.impl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.mori.core.designsystem.MoriLoadingIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.mori.core.model.DisplayFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mori.core.data.ComicPageKey
import com.mori.core.designsystem.LocalExpressiveMotionEnabled
import com.mori.core.designsystem.MoriEmphasized
import com.mori.core.designsystem.MoriIcons
import com.mori.core.designsystem.MoriMotion
import com.mori.core.model.PageFit
import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A full-bleed reader page with pinch-to-zoom, animated double-tap zoom, and zone taps.
 *
 * The page fills the viewport edge to edge on a seamless black bed — no side gaps, no
 * rounded corners, no tonal fillers — so portrait and landscape art alike blend into
 * the reader chrome. Single taps resolve to [ReaderZone] outcomes via [zoneForTap]
 * instead of bubbling to a parent click handler. Taps hold a small
 * double-tap window before firing (single-tap-confirmed): a second
 * contact in-window pairs into a zoom on the way down and neither tap
 * dispatches, so double-taps never deal surprise page turns; held edge taps
 * open a short rhythm window of instant turns for fast skipping. Artwork
 * loads through Coil ([ComicPageKey]) with the page number behind as
 * a placeholder.
 */
@Composable
internal fun ZoomablePage(
    comicId: String,
    pageIndex: Int,
    pageNumber: Int,
    pageFit: PageFit,
    direction: ReadingDirection,
    cropMargins: Boolean,
    onZoneTap: (ReaderZone) -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    onPinchingChange: (Boolean) -> Unit,
    onEdgeTurn: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    half: PageHalf = PageHalf.FULL,
    displayFilter: DisplayFilter = DisplayFilter.Neutral,
) {
    var scale by remember(comicId, pageIndex, pageFit, direction, half) { mutableFloatStateOf(1f) }
    var offset by remember(comicId, pageIndex, pageFit, direction, half) { mutableStateOf(Offset.Zero) }
    // Zoom ownership reporting: the pager stands down while any page is
    // zoomed or pinched so it can never steal the gesture. LaunchedEffect
    // refires on fresh compositions, so recycled pages reset it for free.
    LaunchedEffect(scale > 1f) {
        onZoomedChange(scale > 1f)
    }
    // Aspect of the DECODED art (post-crop, post-split). Fit is computed from
    // these bounds — the minimum scale derives from the image itself, never
    // from a fixed slot. Reset per page identity; the 2:3 placeholder holds
    // layout until decode lands.
    // Without this, decode-side crop would be invisible: Fit normalizes any
    // same-aspect art to the same size inside a fixed box.
    var artAspect by remember(comicId, pageIndex, cropMargins, half) {
        mutableFloatStateOf(PAGE_ASPECT)
    }
    val scope = rememberCoroutineScope()
    // Detector epoch: a direction flip bumps it so a double-tap hold parked
    // across the flip drops instead of dispatching with the old zone.
    val tapEpoch = remember { mutableIntStateOf(1) }
    LaunchedEffect(direction) { tapEpoch.intValue++ }
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    // Serialized motion job: edge pan hops and flings cancel each other
    // instead of fighting over scale/offset. Double-tap zoom runs on its
    // own job (never touch-cancelled, reference-reader parity); a new
    // double-tap retargets it.
    var motionJob by remember { mutableStateOf<Job?>(null) }
    fun launchMotion(block: suspend () -> Unit): Job {
        motionJob?.cancel()
        return scope.launch { block() }.also { motionJob = it }
    }
    var zoomJob by remember { mutableStateOf<Job?>(null) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val density = LocalDensity.current
        val widthPx = remember(density, maxWidth) {
            with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        }
        val heightPx = remember(density, maxHeight) {
            with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        }
        // Latest zoom toggle: the gesture loop below is keyed on direction/width only,
        // so it must read scale through a ref instead of a stale closure. Zooming in
        // centers the tap point (reference-reader focus-center) clamped to the pan
        // bounds; zooming out always returns to fit. Calm motion shortens the glide
        // to a quiet fade. Lives inside the constraints scope so the clamped
        // landing can read the live viewport size.
        val latestZoomToggle = rememberUpdatedState { tap: Offset, center: Offset ->
            val target = zoomTargetForTap(scale)
            val targetOffset = zoomOffsetForTap(tap, center, target, widthPx, heightPx)
            val startScale = scale
            val startOffset = offset
            val spec = if (expressiveMotion) {
                MoriMotion.zoomSpec()
            } else {
                MoriMotion.calmFade()
            }
            zoomJob?.cancel()
            motionJob?.cancel()
            zoomJob = scope.launch {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = spec,
                ) { fraction, _ ->
                    scale = startScale + (target - startScale) * fraction
                    offset = startOffset + (targetOffset - startOffset) * fraction
                }
                if (target <= 1f) {
                    offset = Offset.Zero
                }
            }
        }
        // Live viewport width: the detector loop reads it through state so it
        // survives rotation without restarting mid-tap.
        val viewportWidth = rememberUpdatedState(widthPx)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pageFit(pageFit, artAspect, maxWidth, maxHeight)
                .graphicsLayer {
                    // Render-thread transform: pan/pinch/fling frames never
                    // recompose the page (reference-reader frame path).
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                // Tap detection precedes pan/zoom: a clean tap resolves to a zone
                // before the gesture tracker can claim the press, while pinches
                // (second pointer down) cancel tap tracking and flow below.
                // Claimed tap-ups are consumed so the reader-level fallback detector
                // (see ReaderContent) stands down and each tap dispatches exactly once.
                // While zoomed, edge taps pan toward the tapped side first and turn
                // the page only at the pan limit.
                .zoneTaps(
                    viewportWidth = viewportWidth,
                    direction = direction,
                    scope = scope,
                    epoch = tapEpoch,
                    onZoneTap = { zone ->
                        if ((zone == ReaderZone.PREV || zone == ReaderZone.NEXT) && scale > 1f) {
                            val towardTrailing =
                                (zone == ReaderZone.NEXT) ==
                                    (direction == ReadingDirection.LEFT_TO_RIGHT)
                            when (val decision = panOrTurn(scale, offset.x, widthPx, towardTrailing)) {
                                is PanTurn.Pan -> {
                                    val startX = offset.x
                                    val targetX = decision.targetOffsetX
                                    launchMotion {
                                        animate(
                                            initialValue = startX,
                                            targetValue = targetX,
                                            animationSpec = if (expressiveMotion) {
                                                MoriMotion.pageTurnSpec()
                                            } else {
                                                MoriMotion.calmFade()
                                            },
                                        ) { value, _ ->
                                            offset = offset.copy(x = value)
                                        }
                                    }
                                }
                                PanTurn.Turn -> onZoneTap(zone)
                            }
                        } else {
                            onZoneTap(zone)
                        }
                    },
                    onZoom = { tap, center -> latestZoomToggle.value(tap, center) },
                    consumeUp = true,
                )
                // Pan/zoom routing: single-finger
                // drags at fit pass straight through to the pager; pinches
                // always zoom; pans act only while zoomed and release to the
                // pager at the pan limits (edge handoff).
                .zoomPan(
                    getScale = { scale },
                    setScale = { scale = it },
                    getOffset = { offset },
                    setOffset = { offset = it },
                    onCancelMotion = { motionJob?.cancel() },
                    direction = direction,
                    onEdgeTurn = onEdgeTurn,
                    onPinchingChange = onPinchingChange,
                    onFlingEnd = { velocity ->
                        // Release momentum: ease out over the
                        // velocity-projected target inside the same clamp
                        // the finger obeyed. Serialized with pan hops — a
                        // fresh touch cancels it via onCancelMotion.
                        if (scale > 1f) {
                            val target = flingTarget(offset, velocity, scale, widthPx, heightPx)
                            launchMotion {
                                Animatable(offset, Offset.VectorConverter).animateTo(
                                    targetValue = target,
                                    animationSpec = MoriMotion.flingSpec(),
                                ) { offset = value }
                            }
                        }
                    },
                ),
        ) {
            // Placeholder number behind the art: hidden once the decode
            // lands so it can't ghost through letterbox bars on wide pages.
            var artLoaded by remember(comicId, pageIndex, cropMargins, half) {
                mutableStateOf(false)
            }
            if (!artLoaded) {
                Text(
                    text = pageNumber.toString(),
                    style = MoriEmphasized.headlineSmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
            PageArt(
                comicId = comicId,
                pageIndex = pageIndex,
                pageNumber = pageNumber,
                cropMargins = cropMargins,
                half = half,
                displayFilter = displayFilter,
                onArtSize = { artWidth, artHeight -> artAspect = artAspectFor(artWidth, artHeight) },
                onLoadedChange = { artLoaded = it },
            )
        }
    }
}

/**
 * Page artwork with loading and error states. A failed decode shows a retry
 * affordance instead of failing the whole book; retry restarts the Coil
 * request without touching the cache key.
 */
@Composable
private fun PageArt(
    comicId: String,
    pageIndex: Int,
    pageNumber: Int,
    cropMargins: Boolean,
    half: PageHalf,
    displayFilter: DisplayFilter,
    onArtSize: (widthPx: Float, heightPx: Float) -> Unit,
    onLoadedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(comicId, pageIndex, half) { mutableIntStateOf(0) }
    key(attempt) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, READER_MAX_DIMENSION, cropMargins, half),
            contentScale = ContentScale.Fit,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
        LaunchedEffect(painterState) {
            onLoadedChange(painterState is AsyncImagePainter.State.Success)
        }
        // Intrinsic size follows the decoded (possibly cropped) art; the fit
        // box wraps it so crop refits instead of sitting at the old scale.
        // A state write must not happen during composition, hence the effect.
        val artSize = painter.intrinsicSize
        LaunchedEffect(artSize) {
            if (artSize.isSpecified && artSize.width > 0f && artSize.height > 0f) {
                onArtSize(artSize.width, artSize.height)
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize(),
        ) {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.reader_page_art, pageNumber),
                colorFilter = colorMatrixFor(displayFilter)?.let { ColorFilter.colorMatrix(it) },
                modifier = Modifier.fillMaxSize(),
            )
            // Filter overlays ride above the art (inside the zoom transform):
            // dim, lift, then night warmth. Skipped entirely when neutral.
            if (!displayFilter.isNeutral) {
                val dim = dimAlphaFor(displayFilter.brightness)
                if (dim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dim)),
                    )
                }
                val lift = liftAlphaFor(displayFilter.brightness)
                if (lift > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = lift)),
                    )
                }
                val night = nightAlphaFor(displayFilter.nightTint)
                if (night > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NightTintColor.copy(alpha = night)),
                    )
                }
            }
            when (painterState) {
                is AsyncImagePainter.State.Loading -> MoriLoadingIndicator(
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp),
                )
                is AsyncImagePainter.State.Error -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        imageVector = MoriIcons.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(R.string.reader_page_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(onClick = { attempt++ }) {
                        Text(stringResource(R.string.reader_page_retry))
                    }
                }
                else -> Unit
            }
        }
    }
}

/**
 * Fit container for one page.
 *
 * WIDTH and ORIGINAL fill the viewport width and wrap the DECODED art
 * aspect — so a cropped page refits larger instead of sitting at its old
 * scale inside a fixed slot. Art taller than the viewport is capped at the
 * viewport height (whole art stays reachable: scale-1 never pans, so true
 * overflow would strand content). HEIGHT
 * keeps the viewport box: Fit already maximizes there either way.
 */
private fun Modifier.pageFit(
    fit: PageFit,
    artAspect: Float,
    maxWidth: Dp,
    maxHeight: Dp,
): Modifier = when (fit) {
    PageFit.WIDTH, PageFit.ORIGINAL ->
        fillMaxWidth().height((maxWidth / artAspect).coerceAtMost(maxHeight))
    PageFit.HEIGHT -> fillMaxSize()
}

/**
 * Decoded-art aspect for the fit box, falling back to the placeholder slot
 * for degenerate sizes. Pure for testability.
 */
internal fun artAspectFor(artWidthPx: Float, artHeightPx: Float): Float =
    if (artWidthPx > 0f && artHeightPx > 0f) {
        (artWidthPx / artHeightPx).coerceIn(MIN_ART_ASPECT, MAX_ART_ASPECT)
    } else {
        PAGE_ASPECT
    }

private const val DOUBLE_TAP_ZOOM = 2f

/** Hysteresis on the double-tap toggle: at or below 90% of the zoom level zooms in. */
private const val DOUBLE_TAP_HYSTERESIS = 0.9f
private const val PAGE_ASPECT = 2f / 3f

/** Sanity bounds for decoded-art aspects (guards degenerate intrinsic sizes). */
private const val MIN_ART_ASPECT = 0.2f
private const val MAX_ART_ASPECT = 5f

/**
 * Double-tap zoom target: a 2-state toggle with hysteresis (reference-reader
 * parity). At or below 90% of the zoom level zooms in; anything above —
 * including deep pinches — resets to fit. Pure for testability; the
 * animation itself runs in the page.
 */
internal fun zoomTargetForTap(currentScale: Float): Float =
    if (currentScale <= DOUBLE_TAP_ZOOM * DOUBLE_TAP_HYSTERESIS) DOUBLE_TAP_ZOOM else 1f

/**
 * Translation that glides the tapped art to the viewport center while
 * zooming in (reference-reader focus-center), clamped to the pan bounds so
 * the landing never overshoots into a snap-back on first touch.
 *
 * The page scales about its center, so a point `tap` lands at
 * `center + (tap - center) * scale + offset`; solving for the offset that
 * puts the tap exactly on center gives `(center - tap) * scale`.
 * Zooming out always returns to fit ([Offset.Zero]). Pure for testability.
 */
internal fun zoomOffsetForTap(
    tap: Offset,
    center: Offset,
    targetScale: Float,
    widthPx: Float,
    heightPx: Float,
): Offset {
    if (targetScale <= 1f) return Offset.Zero
    val maxX = widthPx * (targetScale - 1f) / 2f
    val maxY = heightPx * (targetScale - 1f) / 2f
    return Offset(
        ((center.x - tap.x) * targetScale).coerceIn(-maxX, maxX),
        ((center.y - tap.y) * targetScale).coerceIn(-maxY, maxY),
    )
}

/** Longest-side bound for reader page decodes (~10MB worst case in ARGB_8888). */
private const val READER_MAX_DIMENSION = 1600

/** Warm overlay hue for the night filter (alpha carries the strength). */
private val NightTintColor = Color(0xFFFFAB40)
