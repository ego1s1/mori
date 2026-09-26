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
 * Pan/zoom routing for reader pages and its pager:
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
 * drift turns pages mid-zoom. A swipe starting already clamped at the edge
 * in the outward direction passes straight through untouched, so the
 * enabled pager drags it natively with the finger; pans that reach the
 * clamp mid-gesture turn through the explicit overshoot dispatch below.
 *
 * @param quickScale shared hold-drag state for double-tap-hold continuous
 * zoom; null disables it.
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
    quickScale: QuickScaleState? = null,
    /**
     * False while the pager is not allowed to turn: edge pass-through is
     * skipped so the swipe pans (or does nothing) instead of paging.
     */
    swipeToTurn: Boolean = true,
): Modifier = pointerInput(direction) {
    // Engage budget (~5dp: content starts
    // following well before full touch slop. zoneTaps voids holds on the
    // same budget, so taps and pans stay mutually exclusive.
    val engageSlop = GESTURE_ENGAGE_DP * density
    val edgeTurnExtraPx = EDGE_TURN_EXTRA_DP * density
    // Fling gates stay raw px (50px travel, 500px/s): release velocity is
    // already display-px, so density-scaling them would triple the bar on
    // hdpi screens and kill ordinary flicks.
    awaitEachGesture {
        val downs = mutableMapOf<PointerId, Offset>()
        // Release-velocity tracking for the fling: single-finger only,
        // rebased on every finger-count change like the pan math.
        val tracker = VelocityTracker()
        var singlePanned = false
        var pinched = false
        var pastSlop = false
        var prevCount = 0
        // Centroids are kept in SCREEN space. Pointer coords arrive in the
        // layer's local (scaled) space, and translating the layer moves the
        // local coord of a stationary finger — using local deltas as finger
        // deltas feeds the page's own motion back into the gesture, which
        // oscillates (sign-flips every frame → the "shake"). Reconstructing
        // the true screen position each event breaks that feedback loop.
        var prevScreenCentroid = Offset.Zero
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
        // Edge-start pass-through: a fresh single-finger gesture that begins
        // already clamped and pushes further outward belongs to the pager —
        // set once the drift clears engage, cleared on any finger change.
        var edgePassThrough = false
        // Local pointer coord -> true screen coord under the current layer
        // transform (scale about center, then translate). The result is the
        // finger's real screen position, independent of our own translation.
        fun toScreen(local: Offset): Offset {
            val c = Offset(size.width / 2f, size.height / 2f)
            return c + (local - c) * getScale() + getOffset()
        }
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
                    if (pressed.size > 1 && prevCount <= 1 && !pinched) {
                        onPinchingChange(true)
                        pinched = true
                    }
                    val snapshot = toScreen(centroidOf(pressed))
                    prevScreenCentroid = snapshot
                    prevDist = spreadOf(pressed)
                    val joined = prevCount
                    prevCount = pressed.size
                    tracker.resetTracking()
                    edgeOvershoot = 0f
                    flingAnchor = if (pressed.size == 1) snapshot else null
                    flingLast = null
                    if (joined != 0) {
                        // A mid-gesture join/leave ends the hold-drag and any
                        // edge pass-through: pinch (or the fresh single pan)
                        // takes over instead of fighting it.
                        quickScale?.disarm()
                        edgePassThrough = false
                        continue
                    }
                }
                if (pressed.isEmpty()) {
                    // All fingers up: a fast single-finger pan keeps going
                    // with momentum instead of stopping dead. Turns, pinches
                    // and fit-scale have nothing to glide.
                    if (!turned && !pinched && !edgePassThrough && singlePanned && getScale() > 1f) {
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
                    // Pointer coords arrive in the layer's local (unscaled)
                    // space, so drift must be scaled to screen px before it is
                    // compared with the screen-space engage budget — otherwise
                    // the dead zone grows with zoom and pan feels stuck.
                    val engageScale = getScale()
                    var drifted = false
                    for (change in pressed) {
                        val start = downs[change.id] ?: change.position
                        if ((change.position - start).getDistance() * engageScale > engageSlop) {
                            drifted = true
                            break
                        }
                    }
                    if (!drifted) continue
                    pastSlop = true
                    onCancelMotion()
                    if (pressed.size == 1) {
                        val scaleAtEngage = getScale()
                        flingAnchor = toScreen(pressed.first().position)
                        // Edge-start pass-through: a fresh gesture that begins
                        // already clamped and pushes further outward belongs
                        // to the pager outright — claim nothing all gesture
                        // so it drags natively with the finger. Pulling back
                        // pans instead.
                        val first = pressed.first()
                        val start = downs[first.id] ?: first.position
                        val dx = (first.position.x - start.x) * scaleAtEngage
                        edgePassThrough = swipeToTurn && scaleAtEngage > 1f &&
                            isOutwardPush(getOffset().x, scaleAtEngage, size.width.toFloat(), dx)
                    }
                }
                if (edgePassThrough) {
                    // Pager owns this gesture: track baselines only, consume
                    // nothing, so its drag starts on unconsumed slop.
                    prevScreenCentroid = toScreen(centroidOf(pressed))
                    prevDist = spreadOf(pressed)
                    continue
                }
                val multi = pressed.size > 1
                val scaleNow = getScale()
                if (quickScale?.armed == true && !multi) {
                    // Double-tap-hold-drag: vertical travel zooms continuously
                    // around the armed point instead of panning. Horizontal
                    // drift is ignored; a second finger disarms above.
                    val thresholdPx = QUICK_SCALE_THRESHOLD_DP * density
                    val screenCentroid = toScreen(centroidOf(pressed))
                    val y = screenCentroid.y
                    if (quickScale.lastDistance < 0f) {
                        quickScale.lastDistance = abs(quickScale.startY - y) * 2f + thresholdPx
                    }
                    val dist = abs(quickScale.startY - y) * 2f + thresholdPx
                    val mult = quickScaleMultiplier(dist, quickScale.lastDistance, y > prevScreenCentroid.y)
                    if (mult != 1f) {
                        if (!quickScale.moved) {
                            onCancelMotion()
                        }
                        quickScale.markMoved()
                        val newScale = (scaleNow * mult).coerceIn(1f, maxZoom)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // Keep the armed content point fixed while scaling, plus
                        // 1:1 screen-space finger follow (feedback-free: the
                        // anchor is a stored content coord, the delta is a true
                        // screen delta).
                        val cur = getOffset()
                        val uAnchor = quickScale.anchor - center
                        val rawTarget = cur + uAnchor * (scaleNow - newScale) +
                            (screenCentroid - prevScreenCentroid)
                        val target = clampPan(
                            rawTarget,
                            newScale,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                        if (target != cur || newScale != scaleNow) {
                            setScale(newScale)
                            setOffset(target)
                            for (change in pressed) {
                                change.consume()
                            }
                        }
                    }
                    quickScale.sampleSpan(dist)
                    prevScreenCentroid = screenCentroid
                    prevDist = spreadOf(pressed)
                    continue
                }
                if (!multi && scaleNow <= 1f) {
                    // Fit: single-finger drags belong to the pager. Touch nothing.
                    prevScreenCentroid = toScreen(centroidOf(pressed))
                    prevDist = spreadOf(pressed)
                    continue
                }
                val (localCentroid, dist) = centroidAndSpread(pressed)
                val zoom = if (multi && prevDist > 0f) dist / prevDist else 1f
                val targetScale = (scaleNow * zoom).coerceIn(1f, maxZoom)
                val center = Offset(size.width / 2f, size.height / 2f)
                // Screen-space, feedback-free: keep the content point under the
                // centroid while zooming, and move 1:1 with the finger while
                // panning. `screenCentroid` is the finger's true screen position
                // reconstructed from the layer transform, so the page's own
                // translation can never feed back into the delta (that feedback
                // sign-flipped every frame and made slow pans shake).
                val cur = getOffset()
                val screenCentroid = toScreen(localCentroid)
                val rawTarget = zoomPanTarget(
                    current = cur,
                    screenCentroidNow = screenCentroid,
                    screenCentroidPrev = prevScreenCentroid,
                    center = center,
                    scaleNow = scaleNow,
                    targetScale = targetScale,
                )
                val target = clampPan(
                    rawTarget,
                    targetScale,
                    size.width.toFloat(),
                    size.height.toFloat(),
                )
                // Hard stop with fallback: the clamp drops excess, which
                // accumulates toward the explicit turn below. A swipe that
                // started already clamped never reaches here — it passes
                // through to the pager above.
                val used = target != cur || targetScale != scaleNow
                if (!multi) {
                    // Single-finger pan sample for release momentum, in screen
                    // px so the fling gates and glide distance stay correct at
                    // any zoom. Only the traveling finger feeds the tracker;
                    // pinch samples would corrupt the release velocity.
                    val single = pressed.first()
                    val screenPos = toScreen(single.position)
                    tracker.addPosition(single.uptimeMillis, screenPos)
                    flingLast = screenPos
                    singlePanned = true
                }
                if (used && target.x.isFinite() && target.y.isFinite()) {
                    setScale(targetScale)
                    setOffset(target)
                }
                // Own the gesture once engaged and not passing through: consume
                // every change so the pager can neither steal a pan that is
                // still moving nor start/stop flapping as the finger rides the
                // clamp edge. Turning at the edge is the explicit budget below.
                for (change in pressed) {
                    change.consume()
                }
                if (!multi) {
                    val travel = screenCentroid - prevScreenCentroid
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
                        for (change in pressed) {
                            change.consume()
                        }
                        break
                    }
                } else {
                    edgeOvershoot = 0f
                }
                prevScreenCentroid = screenCentroid
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
 * Gesture engage budget in dp: content starts
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
 * Minimum release velocity for a fling (px/s:
 * slower lifts just settle where the finger left them.
 */
internal const val FLING_MIN_VELOCITY_PX_PER_SEC = 500f

/**
 * Minimum finger travel for a fling (px: a fast
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

/** Seconds of release velocity folded into the fling target. */
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

/**
 * Screen-space target translation for one pan/pinch event.
 *
 * Inputs are the finger's true SCREEN centroid positions (reconstructed from
 * the layer transform), not the layer-local pointer coords. Keeping the
 * content point under the centroid while zooming and moving 1:1 with the
 * finger resolves to `T1 = gNow - (gPrev - T0) * (S1 / S0)` with `g` measured
 * from the layer center. Because the deltas are screen-space, the page's own
 * translation can never feed back into the gesture (which previously
 * oscillated and made slow pans shake). Pure for testability.
 */
internal fun zoomPanTarget(
    current: Offset,
    screenCentroidNow: Offset,
    screenCentroidPrev: Offset,
    center: Offset,
    scaleNow: Float,
    targetScale: Float,
): Offset {
    val gNow = screenCentroidNow - center
    val gPrev = screenCentroidPrev - center
    val ratio = targetScale / scaleNow.coerceAtLeast(1e-6f)
    return gNow - (gPrev - current) * ratio
}

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

private fun centroidAndSpread(pressed: List<PointerInputChange>): Pair<Offset, Float> {
    var x = 0f
    var y = 0f
    for (change in pressed) {
        x += change.position.x
        y += change.position.y
    }
    val centroid = Offset(x / pressed.size, y / pressed.size)
    if (pressed.size < 2) return centroid to 0f
    var spread = 0f
    for (change in pressed) {
        spread += (change.position - centroid).getDistance()
    }
    return centroid to spread / pressed.size
}
