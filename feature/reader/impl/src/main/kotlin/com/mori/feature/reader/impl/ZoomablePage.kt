package com.mori.feature.reader.impl

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import com.mori.core.data.ComicPageKey
import com.mori.core.designsystem.MoriMotion
import com.mori.core.model.PageFit
import com.mori.core.model.ReadingDirection
import kotlinx.coroutines.launch

/**
 * A full-bleed reader page with pinch-to-zoom, animated double-tap zoom, and zone taps.
 *
 * The page fills the viewport edge to edge on a seamless black bed — no side gaps, no
 * rounded corners, no tonal fillers — so portrait and landscape art alike blend into
 * the reader chrome. Single taps resolve to [ReaderZone] outcomes via [zoneForTap]
 * instead of bubbling to a parent click handler, so tap navigation and double-tap zoom
 * never double-fire. Artwork loads through Coil ([ComicPageKey]) with the page number
 * behind as a placeholder.
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
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
                .pointerInput(direction) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val fraction = (tapOffset.x / widthPx).coerceIn(0f, 1f)
                            onZoneTap(zoneForTap(fraction, direction))
                        },
                        onDoubleTap = {
                            val target = zoomTargetForTap(scale)
                            scope.launch {
                                animate(
                                    initialValue = scale,
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = DOUBLE_TAP_ZOOM_MS,
                                        easing = MoriMotion.EmphasizedDecelerate,
                                    ),
                                ) { value, _ -> scale = value }
                                if (target <= 1f) {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    )
                }
                .transformable(transformableState),
        ) {
            Text(
                text = pageNumber.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.6f),
            )
            AsyncImage(
                model = ComicPageKey(comicId, pageIndex, READER_MAX_DIMENSION),
                contentDescription = "Page $pageNumber",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun Modifier.pageFit(fit: PageFit): Modifier = when (fit) {
    // Full-bleed width; height follows the loaded art. The placeholder keeps a stable
    // slot so layout does not jump when the bitmap arrives.
    PageFit.WIDTH -> fillMaxWidth().aspectRatio(PAGE_ASPECT)
    PageFit.HEIGHT -> fillMaxSize()
    PageFit.ORIGINAL -> fillMaxWidth()
}

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val PAGE_ASPECT = 2f / 3f
private const val DOUBLE_TAP_ZOOM_MS = 300

/**
 * Double-tap zoom target: zoomed pages reset to fit, unzoomed pages jump to the
 * fixed double-tap level. Pure for testability; the animation itself runs in the page.
 */
internal fun zoomTargetForTap(currentScale: Float): Float =
    if (currentScale > 1f) 1f else DOUBLE_TAP_ZOOM

/** Longest-side bound for reader page decodes (~10MB worst case in ARGB_8888). */
private const val READER_MAX_DIMENSION = 1600
