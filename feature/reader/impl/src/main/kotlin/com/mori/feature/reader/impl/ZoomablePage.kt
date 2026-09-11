package com.mori.feature.reader.impl

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.mori.core.model.ReadingDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A full-bleed reader page with pinch-to-zoom, animated double-tap zoom, and zone taps.
 *
 * The page fills the viewport edge to edge on a seamless black bed — no side gaps, no
 * rounded corners, no tonal fillers — so portrait and landscape art alike blend into
 * the reader chrome. Single taps resolve to [ReaderZone] outcomes via [zoneForTap]
 * instead of bubbling to a parent click handler. Edge taps dispatch on tap-up with
 * no double-tap wait, so rapid taps mid page-turn animation still turn pages;
 * center taps toggle chrome optimistically on tap-up, and a second center tap
 * in-window zooms (toggling chrome back, so a double-tap stays a pure zoom).
 * Artwork loads through Coil ([ComicPageKey]) with the page number behind as
 * a placeholder.
 */
@Composable
internal fun ZoomablePage(
    comicId: String,
    pageIndex: Int,
    pageNumber: Int,
    pageFit: PageFit,
    direction: ReadingDirection,
    onZoneTap: (ReaderZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Zoom/pan state is keyed to the page identity: the pager reuses compositions
    // for neighboring pages, and stale zoom must never leak into a recycled page.
    var scale by remember(comicId, pageIndex, pageFit, direction) { mutableFloatStateOf(1f) }
    var offset by remember(comicId, pageIndex, pageFit, direction) { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    // Serialized motion job: double-tap zoom, edge pan hops, and pinch all
    // cancel each other instead of fighting over scale/offset.
    var motionJob by remember { mutableStateOf<Job?>(null) }
    fun launchMotion(block: suspend () -> Unit) {
        motionJob?.cancel()
        motionJob = scope.launch { block() }
    }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
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
            tween<Float>(
                durationMillis = DOUBLE_TAP_ZOOM_MS,
                easing = MoriMotion.EmphasizedDecelerate,
            )
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
                .pageFit(pageFit)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                )
                // Tap detection precedes transformable: a clean tap resolves to a zone
                // before the transform gesture tracker can claim the press, while pinches
                // (second pointer down) cancel tap tracking and flow to transformable.
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
                                            animationSpec = tween(
                                                durationMillis = EDGE_PAN_MS,
                                                easing = MoriMotion.EmphasizedDecelerate,
                                            ),
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
                    onZoom = { tap, center ->
                        // The first tap already toggled chrome optimistically;
                        // toggling back keeps a double-tap a pure zoom.
                        latestZoomToggle.value(tap, center)
                        onZoneTap(ReaderZone.MENU)
                    },
                    consumeUp = true,
                )
                .transformable(transformableState),
        ) {
            Text(
                text = pageNumber.toString(),
                style = MoriEmphasized.displaySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            PageArt(
                comicId = comicId,
                pageIndex = pageIndex,
                pageNumber = pageNumber,
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
    modifier: Modifier = Modifier,
) {
    var attempt by remember(comicId, pageIndex) { mutableIntStateOf(0) }
    key(attempt) {
        val painter = rememberAsyncImagePainter(
            model = ComicPageKey(comicId, pageIndex, READER_MAX_DIMENSION),
            contentScale = ContentScale.Fit,
        )
        val painterState by painter.state.collectAsStateWithLifecycle()
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

private fun Modifier.pageFit(fit: PageFit): Modifier = when (fit) {
    // Full-bleed width; height follows the loaded art. The placeholder keeps a stable
    // slot so layout does not jump when the bitmap arrives — including ORIGINAL,
    // whose native aspect is unknown until decode.
    PageFit.WIDTH -> fillMaxWidth().aspectRatio(PAGE_ASPECT)
    PageFit.HEIGHT -> fillMaxSize()
    PageFit.ORIGINAL -> fillMaxWidth().aspectRatio(PAGE_ASPECT)
}

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val PAGE_ASPECT = 2f / 3f
private const val DOUBLE_TAP_ZOOM_MS = 350
private const val EDGE_PAN_MS = 180

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
