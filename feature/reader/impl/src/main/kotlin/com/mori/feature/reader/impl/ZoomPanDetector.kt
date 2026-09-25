package com.mori.feature.reader.impl

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.mori.core.model.ReadingDirection

/**
 * Pan/zoom routing for reader pages and its pager:
 *
 * - single-finger drags at fit (`scale <= 1`) are never touched, so the
 *   pager owns swipes outright instead of fighting over every delta;
 * - pinches always zoom (1x–[maxZoom]), anchored on the centroid so the art
 *   stays under the fingers;
 * - single-finger drags while zoomed pan 1:1 inside content bounds with a
 *   hard stop at the clamp — the same gesture never turns the page;
 * - lifting a fast single-finger pan flings with momentum ([onFlingEnd]
 *   carries the release velocity in px/s): the glide decays inside the
 *   same clamp instead of stopping dead, so swipes feel accelerated
 *   rather than glued to the finger;
 * - a *new* swipe starting while already clamped turns the page: if the
 *   first past-slop movement pushes further past the edge, [onEdgeTurn]
 *   fires once ([forward] in reading direction) and the gesture ends. The
 *   swipe may start anywhere on the page, like an unzoomed pager swipe —
 *   only the direction matters. Pulling back into content pans instead.
 *
 * Nothing is consumed before touch slop, so clean taps still resolve to
 * zones in [zoneTaps]. Scale/offset read and write through accessors because
 * the gesture loop outlives recompositions.
 *
 * Ownership notes: the loop reports multi-touch ([onPinchingChange]) so the
 * pager can stand down for the whole pinch — a pager that tracks pinch
 * drift turns pages mid-zoom. The caller turns the page explicitly because
 * the pager is stood down while zoomed and could never take over by itself.
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
    onFlingEnd: (velocityPxPerSec: Offset) -> Unit = {},
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val downs = mutableMapOf<PointerId, Offset>()
        // Release-velocity tracking for the fling: single-finger only,
        // rebased on every finger-count change like the pan math.
        val tracker = VelocityTracker()
        var singlePanned = false
        var pastSlop = false
        var prevCount = 0
        var prevCentroid = Offset.Zero
        var prevDist = 0f
        // Gesture-start snapshot for the second-swipe rule. Read on the
        // first down — NOT at block entry: awaitEachGesture re-arms the
        // block the moment the previous gesture ends (before clocks advance
        // and zoom glides settle), so block-entry reads go stale and a later
        // swipe would turn against pre-animation state.
        var gestureArmed = false
        var gestureStartScale = 1f
        var gestureStartOffset = Offset.Zero
        var edgeTurnFired = false
        try {
            while (true) {
                val event = awaitPointerEvent()
                for (change in event.changes) {
                    if (change.pressed && !change.previousPressed) {
                        downs[change.id] = change.position
                        if (!gestureArmed) {
                            gestureArmed = true
                            gestureStartScale = getScale()
                            gestureStartOffset = getOffset()
                        }
                    }
                }
                val pressed = event.changes.filter { it.pressed }
                event.changes.filter { !it.pressed }.forEach { downs.remove(it.id) }
                if (pressed.isEmpty()) {
                    // All fingers up: a fast single-finger pan keeps going
                    // with momentum instead of stopping dead. Edge-turns and
                    // pinches never fling; fit-scale has nothing to glide.
                    if (!edgeTurnFired && singlePanned && getScale() > 1f) {
                        val raw = tracker.calculateVelocity()
                        val velocity = capFlingVelocity(
                            Offset(raw.x, raw.y),
                            MAX_FLING_PX_PER_SEC,
                        )
                        if (velocity.getDistance() >= MIN_FLING_PX_PER_SEC) {
                            onFlingEnd(velocity)
                        }
                    }
                    break
                }
                if (!pastSlop) {
                    // Tap-friendly: claim nothing until someone actually drifts.
                    val drifted = pressed.any { change ->
                        val start = downs[change.id] ?: change.position
                        (change.position - start).getDistance() > touchSlop
                    }
                    if (!drifted) continue
                    pastSlop = true
                    onCancelMotion()
                    // Second-swipe rule: a fresh single-finger gesture that
                    // begins while already clamped turns when its first
                    // movement pushes further outward — anywhere-origin, like
                    // an unzoomed pager swipe. Pulling back pans instead.
                    if (pressed.size == 1 && gestureStartScale > 1f && !edgeTurnFired) {
                        val first = pressed.first()
                        val start = downs[first.id] ?: first.position
                        val dx = first.position.x - start.x
                        if (isOutwardPush(gestureStartOffset.x, gestureStartScale, size.width.toFloat(), dx)) {
                            edgeTurnFired = true
                            onEdgeTurn(edgeTurnForward(dx, direction))
                            pressed.forEach { it.consume() }
                            break
                        }
                    }
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
                    tracker.resetTracking()
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
                // Hard stop: mid-gesture clamp excess is dropped, never turned.
                val used = target != getOffset() || targetScale != scaleNow
                if (used) {
                    setScale(targetScale)
                    setOffset(target)
                    pressed.forEach { it.consume() }
                    if (!multi) {
                        // Single-finger pan sample for release momentum. Only
                        // the traveling finger feeds the tracker; pinch
                        // samples would corrupt the release velocity.
                        val single = pressed.first()
                        tracker.addPosition(single.uptimeMillis, single.position)
                        singlePanned = true
                    }
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

/**
 * Release-velocity ceiling for pan flings (px/s): a violent swipe still
 * glides instead of teleporting across the clamp. Pure for testability.
 */
internal const val MAX_FLING_PX_PER_SEC = 12_000f

/**
 * Minimum release velocity for a fling (px/s, ~50dp/s at high density):
 * slower lifts just settle where the finger left them. Below typical
 * swipe speeds, so only deliberate lifts-off glide.
 */
internal const val MIN_FLING_PX_PER_SEC = 200f

/**
 * Clamps a release velocity to [maxPxPerSec] magnitude, preserving
 * direction. Pure for testability.
 */
internal fun capFlingVelocity(velocity: Offset, maxPxPerSec: Float = MAX_FLING_PX_PER_SEC): Offset {
    val speed = velocity.getDistance()
    if (speed <= maxPxPerSec || speed <= 0f) return velocity
    return velocity * (maxPxPerSec / speed)
}

/**
 * Whether a fresh swipe pushes further past the already-clamped edge.
 * [offsetX] and [scale] are the gesture-start values; [dx] is the first
 * past-slop horizontal travel. Pure for testability.
 */
internal fun isOutwardPush(offsetX: Float, scale: Float, widthPx: Float, dx: Float): Boolean {
    if (scale <= 1f || dx == 0f) return false
    val maxX = widthPx * (scale - 1f) / 2f
    return when {
        offsetX <= -maxX + EDGE_EPS_PX && dx < 0f -> true
        offsetX >= maxX - EDGE_EPS_PX && dx > 0f -> true
        else -> false
    }
}

/** Clamp tolerance: offsets within a pixel of the limit count as at-edge. */
internal const val EDGE_EPS_PX = 1f

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
