package com.mori.feature.reader.impl

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.TwoWayConverter
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.mori.core.model.DisplayFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    onPinchingChange: (Boolean) -> Unit,
    onEdgeTurn: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    half: PageHalf = PageHalf.FULL,
    displayFilter: DisplayFilter = DisplayFilter.Neutral,
    swipeToTurn: Boolean = true,
) {
    var scale by remember(comicId, pageIndex, pageFit, direction, half) { mutableFloatStateOf(1f) }
    var offset by remember(comicId, pageIndex, pageFit, direction, half) { mutableStateOf(Offset.Zero) }
    // Double-tap-hold-drag state, keyed with the zoom it drives.
    val quickScale = remember(comicId, pageIndex, pageFit, direction, half) { QuickScaleState() }
    // Deep-zoom latch with hysteresis: the hi-res overlay loads past 2x
    // and stays until scale drops back under 1.7x, so pinches hovering at
    // the threshold never thrash the decode.
    var deepZoomLatched by remember(comicId, pageIndex, cropMargins, half) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { scale }.distinctUntilChanged().collect {
            if (it >= DEEP_ZOOM_SCALE) {
                deepZoomLatched = true
            } else if (it < DEEP_ZOOM_EXIT_SCALE) {
                deepZoomLatched = false
            }
        }
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
    val tapEpoch = remember(comicId, pageIndex) { mutableIntStateOf(1) }
    var lastDirection by remember(comicId, pageIndex) { mutableStateOf(direction) }
    LaunchedEffect(direction) {
        if (direction != lastDirection) {
            tapEpoch.intValue++
            lastDirection = direction
        }
    }
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    // Serialized motion job: edge pan hops, flings, and double-tap zooms
    // cancel each other instead of fighting over scale/offset. A fresh touch
    // or a new double-tap retargets via the same cancellation.
    var motionJob by remember { mutableStateOf<Job?>(null) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    fun launchMotion(block: suspend () -> Unit): Job {
        motionJob?.cancel()
        zoomJob?.cancel()
        return scope.launch { block() }.also { motionJob = it }
    }

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
        // centers the tap point clamped to the pan
        // bounds; zooming out always returns to fit. Calm motion snaps instantly.
        // Lives inside the constraints scope so the clamped
        // landing can read the live viewport size.
        val latestZoomToggle = rememberUpdatedState { tap: Offset, center: Offset ->
            val target = zoomTargetForTap(scale)
            val targetOffset = zoomOffsetForTap(tap, center, target, widthPx, heightPx)
            if (!expressiveMotion) {
                zoomJob?.cancel()
                motionJob?.cancel()
                scale = target
                offset = if (target <= 1f) Offset.Zero else targetOffset
            } else {
                zoomJob?.cancel()
                motionJob?.cancel()
                zoomJob = scope.launch {
                    Animatable(ZoomState(scale, offset), ZoomState.VectorConverter).animateTo(
                        targetValue = ZoomState(target, targetOffset),
                        animationSpec = MoriMotion.zoomStateSpec(),
                    ) {
                        scale = value.scale
                        offset = value.offset
                    }
                    if (target <= 1f) {
                        offset = Offset.Zero
                    }
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
                    // recompose the page.
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
                                    val targetX = decision.targetOffsetX
                                    if (!expressiveMotion) {
                                        motionJob?.cancel()
                                        zoomJob?.cancel()
                                        offset = offset.copy(x = targetX)
                                    } else {
                                        launchMotion {
                                            animate(
                                                initialValue = offset.x,
                                                targetValue = targetX,
                                                animationSpec = MoriMotion.pageTurnSpec(),
                                            ) { value, _ ->
                                                offset = offset.copy(x = value)
                                            }
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
                    quickScale = quickScale,
                )
    // Pan/zoom routing: single-finger drags at fit pass straight through
    // to the pager; edge-start outward swipes do the same so the pager
    // drags them natively. Pinches always zoom; pans act only while zoomed,
    // turning explicitly past the clamp when the pager declines.
                .zoomPan(
                    getScale = { scale },
                    setScale = { scale = it },
                    getOffset = { offset },
                    setOffset = { offset = it },
                    onCancelMotion = {
                        motionJob?.cancel()
                        zoomJob?.cancel()
                    },
                    direction = direction,
                    onEdgeTurn = onEdgeTurn,
                    onPinchingChange = onPinchingChange,
                    quickScale = quickScale,
                    swipeToTurn = swipeToTurn,
                    onFlingEnd = { velocity ->
                        // Release momentum: ease out over the
                        // velocity-projected target inside the same clamp
                        // the finger obeyed. Serialized with pan hops — a
                        // fresh touch cancels it via onCancelMotion. Calm
                        // motion snaps instead of gliding.
                        if (scale > 1f) {
                            val target = flingTarget(offset, velocity, scale, widthPx, heightPx)
                            if (!expressiveMotion) {
                                offset = target
                            } else {
                                launchMotion {
                                    Animatable(offset, Offset.VectorConverter).animateTo(
                                        targetValue = target,
                                        animationSpec = MoriMotion.flingSpec(),
                                    ) { offset = value }
                                }
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
                deepZoom = deepZoomLatched,
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
 *
 * @param deepZoom when true, a higher-resolution overlay fades in over the
 * base art for sharp deep zoom (silent fallback: base art stays on failure).
 */
@Composable
private fun PageArt(
    comicId: String,
    pageIndex: Int,
    pageNumber: Int,
    cropMargins: Boolean,
    half: PageHalf,
    displayFilter: DisplayFilter,
    deepZoom: Boolean,
    onArtSize: (widthPx: Float, heightPx: Float) -> Unit,
    onLoadedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(comicId, pageIndex, cropMargins, half) { mutableIntStateOf(0) }
    key(attempt) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, READER_MAX_DIMENSION, cropMargins, half),
            contentScale = ContentScale.Fit,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
        val painterSuccess = painterState is AsyncImagePainter.State.Success
        LaunchedEffect(painterSuccess) {
            onLoadedChange(painterSuccess)
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
            // Deep-zoom sharpness: past 2x a higher-resolution decode fades
            // in over the base art (same key family, Coil-cached). The base
            // stays underneath until the hi-res lands, so there is no flash.
            if (deepZoom) {
                HiResOverlay(
                    comicId = comicId,
                    pageIndex = pageIndex,
                    cropMargins = cropMargins,
                    half = half,
                    displayFilter = displayFilter,
                )
            }
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

/**
 * Combined scale + offset animation state for seamless double-tap retargets:
 * a fresh tap animates from the live position instead of restarting the lerp.
 */
private data class ZoomState(val scale: Float, val offset: Offset) {
    companion object {
        val VectorConverter: TwoWayConverter<ZoomState, AnimationVector3D> = TwoWayConverter(
            convertToVector = { AnimationVector3D(it.scale, it.offset.x, it.offset.y) },
            convertFromVector = { ZoomState(it.v1, Offset(it.v2, it.v3)) },
        )
    }
}

/** Sanity bounds for decoded-art aspects (guards degenerate intrinsic sizes). */
private const val MIN_ART_ASPECT = 0.2f
private const val MAX_ART_ASPECT = 5f

/**
 * Double-tap zoom target: a 2-state toggle with hysteresis. At or below 90% of the zoom level zooms in; anything above —
 * including deep pinches — resets to fit. Pure for testability; the
 * animation itself runs in the page.
 */
internal fun zoomTargetForTap(currentScale: Float): Float =
    if (currentScale <= DOUBLE_TAP_ZOOM * DOUBLE_TAP_HYSTERESIS) DOUBLE_TAP_ZOOM else 1f

/**
 * Translation that glides the tapped art to the viewport center while
 * zooming in, clamped to the pan bounds so
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

/**
 * Deep-zoom decode bound past [DEEP_ZOOM_SCALE]: one sharper bitmap swapped
 * over the base art instead of stretching it (~39MB transient worst case at
 * 2:3 portrait, Coil-cached, released with the page).
 */
private const val HI_RES_DIMENSION = 2560
private const val HI_RES_DIMENSION_LOW_RAM = 1280

private fun isLowRamDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return activityManager?.isLowRamDevice ?: false
}

/** Scale past which the hi-res overlay loads. */
private const val DEEP_ZOOM_SCALE = 2f

/** Scale below which the hi-res overlay unloads (hysteresis vs [DEEP_ZOOM_SCALE]). */
private const val DEEP_ZOOM_EXIT_SCALE = 1.7f

/**
 * Higher-resolution overlay for deep zoom. Loads only while zoomed past
 * [DEEP_ZOOM_SCALE] and draws only once decoded, over the base art with the
 * same filter — no placeholder, no flash, no layout effect.
 */
@Composable
private fun HiResOverlay(
    comicId: String,
    pageIndex: Int,
    cropMargins: Boolean,
    half: PageHalf,
    displayFilter: DisplayFilter,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hiResDimension = remember(context) {
        if (isLowRamDevice(context)) HI_RES_DIMENSION_LOW_RAM else HI_RES_DIMENSION
    }
    key(comicId, pageIndex, cropMargins, half) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, hiResDimension, cropMargins, half),
            contentScale = ContentScale.Fit,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
        if (painterState is AsyncImagePainter.State.Success) {
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = colorMatrixFor(displayFilter)?.let { ColorFilter.colorMatrix(it) },
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

/** Warm overlay hue for the night filter (alpha carries the strength). */
private val NightTintColor = Color(0xFFFFAB40)
