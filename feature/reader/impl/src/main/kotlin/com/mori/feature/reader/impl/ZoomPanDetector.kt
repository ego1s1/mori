package com.mori.feature.reader.impl

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.mori.core.model.ReadingDirection
import kotlin.math.abs

/**
 * Pan/zoom routing for reader pages, mirroring Mihon's PhotoView contract
 * with its pager:
 *
 * - single-finger drags at fit (`scale <= 1`) are never touched, so the
 *   pager owns swipes outright instead of fighting over every delta;
 * - pinches always zoom (1x–[maxZoom]), anchored on the centroid so the art
 *   stays under the fingers;
 * - single-finger drags while zoomed pan inside content bounds; anything the
 *   clamp eats is left unconsumed so the pager takes over at the pan limits
 *   (edge handoff).
 *
 * Nothing is consumed before touch slop, so clean taps still resolve to
 * zones in [zoneTaps]. Scale/offset read and write through accessors because
 * the gesture loop outlives recompositions.
 *
 * Ownership notes: the loop reports multi-touch ([onPinchingChange]) so the
 * pager can stand down for the whole pinch — a pager that tracks pinch
 * drift turns pages mid-zoom. Edge pushes past the clamp fire
 * [onEdgeTurn] exactly once per gesture ([forward] in reading direction);
 * the caller turns the page explicitly because the pager is stood down
 * while zoomed and could never take over by itself.
 */
internal fun Modifier.zoomPan(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getOffset: () -> Offset,
    setOffset: (Offset) -> Unit,
    onCancelMotion: () -> Unit,
    direction: ReadingDirection,
    onEdgeTurn: (forward: Boolean) -> Unit,
    onPinchingChange: (pinching: Boolean) -> Unit,
    maxZoom: Float = MAX_ZOOM,
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    // Push-past-edge travel that commits to a page turn. Several slops so a
    // shaky hold at the clamp never turns by accident.
    val edgeSlip = touchSlop * EDGE_TURN_SLOP_MULTIPLE
    awaitEachGesture {
        val downs = mutableMapOf<PointerId, Offset>()
        var pastSlop = false
        var prevCount = 0
        var prevCentroid = Offset.Zero
        var prevDist = 0f
        var edgeFired = false
        // Signed push accumulated past the clamp. Per-frame excess is tiny
        // (one frame of travel), so a single frame can never commit — only
        // sustained pushing in one direction trips the turn. Jitter cancels
        // itself out; reversing restarts the count.
        var edgeDebt = 0f
        try {
            while (true) {
                val event = awaitPointerEvent()
                for (change in event.changes) {
                    if (change.pressed && !change.previousPressed) {
                        downs[change.id] = change.position
                    }
                }
                val pressed = event.changes.filter { it.pressed }
                event.changes.filter { !it.pressed }.forEach { downs.remove(it.id) }
                if (pressed.isEmpty()) break
                if (!pastSlop) {
                    // Tap-friendly: claim nothing until someone actually drifts.
                    val drifted = pressed.any { change ->
                        val start = downs[change.id] ?: change.position
                        (change.position - start).getDistance() > touchSlop
                    }
                    if (!drifted) continue
                    pastSlop = true
                    onCancelMotion()
                }
                if (pressed.size != prevCount) {
                    // Fresh finger(s): rebase so counts never jump the content.
                    // A second finger also arms the pinch report so the pager
                    // stands down for the whole multi-touch gesture.
                    if (pressed.size > 1) onPinchingChange(true)
                    val centroid = centroidOf(pressed)
                    prevCentroid = centroid
                    prevDist = spreadOf(pressed)
                    prevCount = pressed.size
                    continue
                }
                val multi = pressed.size > 1
                val scaleNow = getScale()
                if (!multi && scaleNow <= 1f) {
                    // Fit: single-finger drags belong to the pager. Touch nothing.
                    val centroid = centroidOf(pressed)
                    prevCentroid = centroid
                    prevDist = spreadOf(pressed)
                    continue
                }
                val centroid = centroidOf(pressed)
                val dist = spreadOf(pressed)
                val zoom = if (multi && prevDist > 0f) dist / prevDist else 1f
                val targetScale = (scaleNow * zoom).coerceIn(1f, maxZoom)
                val center = Offset(size.width / 2f, size.height / 2f)
                // Scale about the centroid so the tapped art stays put, then
                // follow centroid travel, all inside content bounds.
                val f = centroid - center
                val ratio = targetScale / scaleNow.coerceAtLeast(1e-6f)
                val rawTarget = f * (1f - ratio) + getOffset() * ratio + (centroid - prevCentroid)
                val target = clampPan(rawTarget, targetScale, size.width.toFloat(), size.height.toFloat())
                if (!multi && scaleNow > 1f && !edgeFired) {
                    val overshootX = rawTarget.x - target.x
                    edgeDebt = when {
                        overshootX == 0f -> 0f
                        edgeDebt == 0f || overshootX * edgeDebt > 0f -> edgeDebt + overshootX
                        else -> overshootX
                    }
                    if (abs(edgeDebt) > edgeSlip) {
                        edgeFired = true
                        onEdgeTurn(edgeTurnForward(overshootX, direction))
                    }
                }
                val used = target != getOffset() || targetScale != scaleNow
                if (used) {
                    setScale(targetScale)
                    setOffset(target)
                    pressed.forEach { it.consume() }
                }
                prevCentroid = centroid
                prevDist = dist
            }
        } finally {
            // Gesture over (or detector gone): the pager may scroll again.
            onPinchingChange(false)
        }
    }
}

/** Pinch ceiling shared by manual pan/zoom and the double-tap toggle. */
internal const val MAX_ZOOM = 4f

/** Push-past-edge travel (in touch slops) that commits to a page turn. */
internal const val EDGE_TURN_SLOP_MULTIPLE = 3f

/** Maps a horizontal clamp overshoot to reading-direction travel. Pure for testability. */
internal fun edgeTurnForward(overshootX: Float, direction: ReadingDirection): Boolean =
    if (direction == ReadingDirection.LEFT_TO_RIGHT) overshootX < 0f else overshootX > 0f

/** Pan limits for width-fitted content: overflow halves each side. */
internal fun clampPan(target: Offset, scale: Float, widthPx: Float, heightPx: Float): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = widthPx * (scale - 1f) / 2f
    val maxY = heightPx * (scale - 1f) / 2f
    return Offset(
        target.x.coerceIn(-maxX, maxX),
        target.y.coerceIn(-maxY, maxY),
    )
}

private fun centroidOf(pressed: List<PointerInputChange>): Offset {
    var x = 0f
    var y = 0f
    pressed.forEach {
        x += it.position.x
        y += it.position.y
    }
    return Offset(x / pressed.size, y / pressed.size)
}

private fun spreadOf(pressed: List<PointerInputChange>): Float {
    if (pressed.size < 2) return 0f
    val centroid = centroidOf(pressed)
    var spread = 0f
    pressed.forEach { spread += (it.position - centroid).getDistance() }
    return spread / pressed.size
}
