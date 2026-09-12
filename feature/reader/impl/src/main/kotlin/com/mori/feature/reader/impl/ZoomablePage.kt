package com.mori.feature.reader.impl

import androidx.compose.animation.core.animate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * double-tap window before firing (Mihon single-tap-confirmed): a second
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
    modifier: Modifier = Modifier,
    half: PageHalf = PageHalf.FULL,
) {
    // Zoom/pan state is keyed to the page identity: the pager reuses compositions
    // for neighboring pages, and stale zoom must never leak into a recycled page.
    // Split halves are distinct identities — each half zooms on its own.
    var scale by remember(comicId, pageIndex, pageFit, direction, half) { mutableFloatStateOf(1f) }
    var offset by remember(comicId, pageIndex, pageFit, direction, half) { mutableStateOf(Offset.Zero) }
    // Aspect of the DECODED art (post-crop, post-split). Fit is computed from
    // these bounds — Mihon's model, where SubsamplingScaleImageView derives
    // its minimum scale from the image, never from a fixed slot. Reset per
    // page identity; the 2:3 placeholder holds layout until decode lands.
    // Without this, decode-side crop would be invisible: Fit normalizes any
    // same-aspect art to the same size inside a fixed box.
    var artAspect by remember(comicId, pageIndex, cropMargins, half) {
        mutableFloatStateOf(PAGE_ASPECT)
    }
    val scope = rememberCoroutineScope()
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    // Serialized motion job: double-tap zoom, edge pan hops, and pinch all
    // cancel each other instead of fighting over scale/offset.
    var motionJob by remember { mutableStateOf<Job?>(null) }
    fun launchMotion(block: suspend () -> Unit) {
        motionJob?.cancel()
        motionJob = scope.launch { block() }
    }
    // Latest zoom toggle: the gesture loop below is keyed on direction/width only,
    // so it must read scale through a ref instead of a stale closure. Zooming in
    // anchors on the tap point (the tapped art stays under the finger); zooming
    // out always returns to fit. Calm motion shortens the glide to a quiet fade.
    val latestZoomToggle = rememberUpdatedState { tap: Offset, center: Offset ->
        val target = zoomTargetForTap(scale)
        val targetOffset = zoomOffsetForTap(tap, center, target)
        val startScale = scale
        val startOffset = offset
        val spec = if (expressiveMotion) {
            MoriMotion.zoomSpec()
        } else {
            MoriMotion.calmFade()
        }
        launchMotion {
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
        // Live viewport width: the detector loop reads it through state so it
        // survives rotation without restarting mid-tap.
        val viewportWidth = rememberUpdatedState(widthPx)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pageFit(pageFit, artAspect, maxWidth, maxHeight)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
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
                                            animationSpec = MoriMotion.pageTurnSpec(),
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
                // Pan/zoom routing (Mihon PhotoView parity): single-finger
                // drags at fit pass straight through to the pager; pinches
                // always zoom; pans act only while zoomed and release to the
                // pager at the pan limits (edge handoff).
                .zoomPan(
                    getScale = { scale },
                    setScale = { scale = it },
                    getOffset = { offset },
                    setOffset = { offset = it },
                    onCancelMotion = { motionJob?.cancel() },
                ),
        ) {
            Text(
                text = pageNumber.toString(),
                style = MoriEmphasized.headlineSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            PageArt(
                comicId = comicId,
                pageIndex = pageIndex,
                pageNumber = pageNumber,
                cropMargins = cropMargins,
                half = half,
                onArtSize = { artWidth, artHeight -> artAspect = artAspectFor(artWidth, artHeight) },
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
    onArtSize: (widthPx: Float, heightPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(comicId, pageIndex, half) { mutableIntStateOf(0) }
    key(attempt) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, READER_MAX_DIMENSION, cropMargins, half),
            contentScale = ContentScale.Fit,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
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
                modifier = Modifier.fillMaxSize(),
            )
            when (painterState) {
                is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
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
 * overflow would strand content, unlike Mihon's pannable view). HEIGHT
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

private const val DOUBLE_TAP_ZOOM = 2.5f
private const val PAGE_ASPECT = 2f / 3f

/** Sanity bounds for decoded-art aspects (guards degenerate intrinsic sizes). */
private const val MIN_ART_ASPECT = 0.2f
private const val MAX_ART_ASPECT = 5f

/**
 * Double-tap zoom target: zoomed pages reset to fit, unzoomed pages jump to the
 * fixed double-tap level. Pure for testability; the animation itself runs in the page.
 */
internal fun zoomTargetForTap(currentScale: Float): Float =
    if (currentScale > 1f) 1f else DOUBLE_TAP_ZOOM

/**
 * Translation that keeps the tapped art under the finger while zooming in.
 *
 * The page scales about its center, so without compensation the tap point drifts
 * outward; shifting by `(tap - center) * (1 - scale)` cancels the drift exactly.
 * Zooming out always returns to fit ([Offset.Zero]). Pure for testability.
 */
internal fun zoomOffsetForTap(tap: Offset, center: Offset, targetScale: Float): Offset =
    if (targetScale <= 1f) Offset.Zero else (tap - center) * (1f - targetScale)

/** Longest-side bound for reader page decodes (~10MB worst case in ARGB_8888). */
private const val READER_MAX_DIMENSION = 1600
