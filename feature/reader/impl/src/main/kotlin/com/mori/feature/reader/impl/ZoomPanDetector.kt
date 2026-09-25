package com.mori.feature.reader.impl

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.mori.core.model.ReadingDirection
import kotlin.math.abs

/**
 * Pan/zoom routing for reader pages and its pager (reference-reader parity):
 *
 * - single-finger drags at fit (`scale <= 1`) are never touched, so the
 *   pager owns swipes outright instead of fighting over every delta;
 * - engagement is ~5dp of drift (not full touch slop), so content follows
 *   the finger with no dead zone; taps still resolve below it in [zoneTaps];
 * - pinches always zoom (1x–[maxZoom]), anchored on the centroid so the art
 *   stays under the fingers;
 * - single-finger drags while zoomed pan 1:1 inside content bounds. Pushing
 *   further past a clamped edge turns the page mid-gesture (past a small
 *   overshoot budget) — the same swipe that pans also pages, anywhere-origin,
 *   direction-only. Pulling back pans instead;
 * - lifting a fast single-finger pan flings with momentum ([onFlingEnd]
 *   carries the release velocity in px/s): the glide eases out over the
 *   velocity-projected target inside the same clamp instead of stopping dead.
 *
 * Nothing is consumed before the engage threshold, so clean taps still
 * resolve to zones in [zoneTaps]. Scale/offset read and write through
 * accessors because the gesture loop outlives recompositions.
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
    // Engage budget (~5dp, reference-reader parity): content starts
    // following well before full touch slop. zoneTaps voids holds on the
    // same budget, so taps and pans stay mutually exclusive.
    val engageSlop = GESTURE_ENGAGE_DP * density
    val edgeTurnExtraPx = EDGE_TURN_EXTRA_DP * density
    awaitEachGesture {
        val downs = mutableMapOf<PointerId, Offset>()
        // Release-velocity tracking for the fling: single-finger only,
        // rebased on every finger-count change like the pan math.
        val tracker = VelocityTracker()
        var singlePanned = false
        var pinched = false
        var pastSlop = false
        var prevCount = 0
        var prevCentroid = Offset.Zero
        var prevDist = 0f
        // Finger travel of the pan portion, for the fling displacement
        // gate (reference readers require both speed and travel).
        var flingAnchor: Offset? = null
        var flingLast: Offset? = null
        // Overshoot past the clamp this gesture: pushing further outward
        // past the budget turns the page mid-gesture. Any inward or
        // vertical drift resets it.
        var edgeOvershoot = 0f
        var turned = false
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
                if (pressed.size != prevCount && pressed.isNotEmpty()) {
                    // Fresh finger(s): snapshot the baseline at contact time so
                    // the slop-crossing move pans from the down point instead
                    // of rebasing its travel away. A second finger also arms
                    // the pinch report so the pager stands down for the whole
                    // multi-touch gesture. Only a mid-gesture join/leave skips
                    // panning for one event (stale centroid would jump).
                    if (pressed.size > 1 && prevCount <= 1) {
                        onPinchingChange(true)
                        pinched = true
                    }
                    val snapshot = centroidOf(pressed)
                    prevCentroid = snapshot
                    prevDist = spreadOf(pressed)
                    val joined = prevCount
                    prevCount = pressed.size
                    tracker.resetTracking()
                    edgeOvershoot = 0f
                    flingAnchor = if (pressed.size == 1) snapshot else null
                    flingLast = null
                    if (joined != 0) continue
                }
                if (pressed.isEmpty()) {                    // All fingers up: a fast single-finger pan keeps going
                    // with momentum instead of stopping dead. Turns, pinches
                    // and fit-scale have nothing to glide.
                    if (!turned && !pinched && singlePanned && getScale() > 1f) {
                        val raw = tracker.calculateVelocity()
                        val velocity = capFlingVelocity(
                            Offset(raw.x, raw.y),
                            MAX_FLING_PX_PER_SEC,
                        )
                        val anchor = flingAnchor
                        val last = flingLast
                        val travel = if (anchor != null && last != null) {
                            (last - anchor).getDistance()
                        } else {
                            0f
                        }
                        if (travel >= FLING_MIN_TRAVEL_PX &&
                            velocity.getDistance() >= FLING_MIN_VELOCITY_PX_PER_SEC
                        ) {
                            onFlingEnd(velocity)
                        }
                    }
                    break
                }
                if (!pastSlop) {
                    // Tap-friendly: claim nothing until someone actually drifts.
                    val drifted = pressed.any { change ->
                        val start = downs[change.id] ?: change.position
                        (change.position - start).getDistance() > engageSlop
                    }
                    if (!drifted) continue
                    pastSlop = true
                    onCancelMotion()
                    if (pressed.size == 1) {
                        flingAnchor = pressed.first().position
                    }
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
                // Hard stop: mid-gesture clamp excess is dropped, never turned —
                // unless it keeps pushing outward past the turn budget.
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
                        flingLast = single.position
                        singlePanned = true
                        // Edge handoff: the dropped clamp excess accumulates
                        // only while the push stays horizontal and outward.
                        // Past the budget the same swipe pages — anywhere
                        // origin, direction only. Anything else resets.
                        val travel = centroid - prevCentroid
                        val horizontal = abs(travel.x) > abs(travel.y)
                        edgeOvershoot = accumulateEdgeOvershoot(
                            edgeOvershoot,
                            excessX = rawTarget.x - target.x,
                            clampedX = target.x,
                            horizontalPush = horizontal,
                        )
                        if (edgeOvershoot >= edgeTurnExtraPx) {
                            turned = true
                            onEdgeTurn(edgeTurnForward(rawTarget.x - target.x, direction))
                            pressed.forEach { it.consume() }
                            break
                        }
                    } else {
                        edgeOvershoot = 0f
                    }
                } else if (!multi) {
                    // Fully clamped with no room: still watch the push, so a
                    // swipe starting pinned at the edge can turn without
                    // content moving first.
                    val travel = centroid - prevCentroid
                    val horizontal = abs(travel.x) > abs(travel.y)
                    edgeOvershoot = accumulateEdgeOvershoot(
                        edgeOvershoot,
                        excessX = rawTarget.x - target.x,
                        clampedX = target.x,
                        horizontalPush = horizontal,
                    )
                    if (edgeOvershoot >= edgeTurnExtraPx) {
                        turned = true
                        onEdgeTurn(edgeTurnForward(rawTarget.x - target.x, direction))
                        pressed.forEach { it.consume() }
                        break
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
internal const val MAX_ZOOM = 5f

/**
 * Gesture engage budget in dp (reference-reader parity): content starts
 * following well before full touch slop. Shared with [zoneTaps], which
 * voids tap holds on the same budget so taps and pans stay exclusive.
 */
internal const val GESTURE_ENGAGE_DP = 5f

/**
 * Overshoot past the clamp that turns the page mid-gesture, in dp: the
 * same swipe that pans keeps pushing into a turn. Resets on any inward
 * or vertical drift.
 */
internal const val EDGE_TURN_EXTRA_DP = 8f

/**
 * Release-velocity ceiling for pan flings (px/s): a violent swipe still
 * glides instead of teleporting across the clamp. Pure for testability.
 */
internal const val MAX_FLING_PX_PER_SEC = 12_000f

/**
 * Minimum release velocity for a fling (px/s, reference-reader parity):
 * slower lifts just settle where the finger left them.
 */
internal const val FLING_MIN_VELOCITY_PX_PER_SEC = 500f

/**
 * Minimum finger travel for a fling (px, reference-reader parity): a fast
 * flick with no travel is a tap, not a glide.
 */
internal const val FLING_MIN_TRAVEL_PX = 50f

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
 * Grows the edge-turn overshoot budget while a clamped push stays
 * horizontal and outward; any inward, vertical, or unclamped drift resets
 * it, so stray wiggles never turn the page. Pure for testability.
 */
internal fun accumulateEdgeOvershoot(
    currentPx: Float,
    excessX: Float,
    clampedX: Float,
    horizontalPush: Boolean,
): Float =
    if (horizontalPush && clampedX != 0f && excessX * clampedX > 0f) {
        currentPx + abs(excessX)
    } else {
        0f
    }

/** Seconds of release velocity folded into the fling target (reference-reader parity). */
internal const val FLING_GLIDE_SECONDS = 0.25f

/**
 * Fling landing point: current position plus a quarter-second of release
 * velocity, inside the same clamp the finger obeyed. The glide animation
 * eases out over it. Pure for testability.
 */
internal fun flingTarget(
    start: Offset,
    velocityPxPerSec: Offset,
    scale: Float,
    widthPx: Float,
    heightPx: Float,
): Offset = clampPan(start + velocityPxPerSec * FLING_GLIDE_SECONDS, scale, widthPx, heightPx)

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
