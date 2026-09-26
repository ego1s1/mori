package com.mori.feature.reader.impl

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import com.mori.core.model.ReadingDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Zone-tap detector shared by reader pages and the reader container.
 *
 * Tap zones resolve against the viewport ([viewportWidth]), so a tap anywhere —
 * on a page, on the black bed between pages mid page-turn, or in the letterbox
 * margins on wide screens — always navigates. Detectors on narrower centered
 * content rebase their local tap position into viewport space, so the same
 * physical x always lands in the same zone.
 *
 * Single-tap-confirmed dispatch: every tap holds a small double-tap
 * window before it fires. A second contact landing in-window arms a
 * quick-scale hold-drag (continuous zoom while held, stepped zoom on clean
 * lift) — like the platform `onDoubleTap` — so pairs never dispatch and
 * there is nothing to undo or compensate. Held edge taps
 * open a short rhythm window of instant turns for fast skipping; center taps
 * always hold, so double-tap-to-zoom works from any state; a press that drifts
 * into a scroll or pinch stands the hold down silently.
 *
 * Every finger is tracked independently inside one gesture: alternating
 * two-finger skipping dispatches one tap per finger-up, even with overlapping
 * contact (overlapping downs never pair — pairs are strictly sequential).
 * A press only counts when its own drift stays within the engage budget, no
 * other finger drifted either (pinches void the whole gesture), and the up
 * wasn't claimed by a drag handler. Presses that drift past the budget are
 * not taps and are ignored. The trailing up of a paired double-tap is
 * swallowed.
 *
 * All pairing math runs on pointer-event time, so tests drive it with the
 * test clock. [viewportWidth] is read live through state, so the detector
 * loop (keyed on [direction] only) survives rotation and resize without
 * dropping in-flight taps. When [consumeUp] is true the tap-up is consumed
 * after handling, so an outer detector further up the hit path ignores taps
 * already claimed by a page. Taps swallowed by chrome clickables never reach
 * a detector at all.
 *
 * @param quickScale shared hold-drag state; null keeps the immediate
 * stepped zoom on pair (container fallback).
 */
internal fun Modifier.zoneTaps(
    viewportWidth: State<Float>,
    direction: ReadingDirection,
    scope: CoroutineScope,
    /**
     * Bumped by the caller whenever [direction] changes. The hold job runs
     * in [scope] (a pointerInput restart cannot cancel outer-scope
     * children), so it stamps the epoch at hold time and drops the fire
     * when the epoch moved — a direction flip mid-hold moves it, and the
     * stale fire drops.
     */
    epoch: State<Int>,
    onZoneTap: (ReaderZone) -> Unit,
    onZoom: (tap: Offset, center: Offset) -> Unit,
    consumeUp: Boolean,
    quickScale: QuickScaleState? = null,
): Modifier = pointerInput(direction) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeoutMs = android.view.ViewConfiguration.getLongPressTimeout().toLong()
    // Same engage budget as zoomPan: a press that drifts past it is a
    // gesture, never a tap — the two detectors stay mutually exclusive.
    // Pair radius keeps 2x slop so double-taps stay forgiving.
    val engageSlop = GESTURE_ENGAGE_DP * density
    var pendingTap: TapRecord? = null
    var holdJob: Job? = null
    // Epoch stamped with the hold: pairing and rhythm only continue on the
    // same direction epoch, so a flip mid-gesture never dispatches stale.
    var holdEpoch = 0
    var lastRhythmEdgeMs = 0L
    var lastRhythmZone: ReaderZone? = null
    var lastRhythmEpoch = 0
    val consumedIds = mutableSetOf<PointerId>()

    fun cancelHold() {
        holdJob?.cancel()
        holdJob = null
        pendingTap = null
    }

    /**
     * Fires a held tap; edge taps open the instant-rhythm window stamped at
     * fire time (not tap time) so the window length is consistent whether
     * the tap fired after the hold or via early confirm. Rhythm resumes only
     * for the same zone — a PREV then NEXT must both hold, never instant-fire.
     */
    fun fireHeld(tap: TapRecord, nowMs: Long = tap.timeMs) {
        if (tap.zone != ReaderZone.MENU) {
            lastRhythmEdgeMs = nowMs
            lastRhythmZone = tap.zone
            lastRhythmEpoch = epoch.value
        }
        onZoneTap(tap.zone)
    }

    /**
     * Pairs a fresh down with a held tap-up (double-tap timing): arms
     * quick-scale instead of zooming immediately, so a hold-drag zooms
     * continuously while a clean lift falls through to the stepped zoom.
     */
    fun pairOnDown(id: PointerId, position: Offset, center: Offset, downMs: Long, downConsumed: Boolean): Boolean {
        if (downConsumed) return false
        val armed = pendingTap ?: return false
        if (holdEpoch != epoch.value) return false
        if (!shouldPair(armed, downMs, position, touchSlop)) {
            return false
        }
        cancelHold()
        consumedIds += id
        quickScale?.arm(id, position.y, position)
        if (quickScale == null) {
            onZoom(position, center)
        }
        return true
    }

    fun handleTap(
        position: Offset,
        center: Offset,
        zone: ReaderZone,
        nowMs: Long,
        canPair: Boolean,
    ) {
        val armed = pendingTap
        if (canPair && holdEpoch == epoch.value && shouldPair(armed, nowMs, position, touchSlop)) {
            // Double-tap completed on lift (the down already paired when the
            // frames arrived separately): zoom, and neither tap dispatches —
            // the first was held, so there is nothing to undo.
            cancelHold()
            onZoom(position, center)
            return
        }
        // Early confirm: a far or overlapping second tap rules out pairing,
        // so fire the held tap now instead of waiting out the window. The
        // current tap always holds afterwards — it must never instant-fire
        // on the rhythm the confirm just opened.
        val earlyConfirmed = armed != null
        if (earlyConfirmed) {
            cancelHold()
            fireHeld(armed, nowMs)
        }
        if (!earlyConfirmed && zone != ReaderZone.MENU && lastRhythmZone == zone &&
            lastRhythmEpoch == epoch.value && isRhythmActive(lastRhythmEdgeMs, nowMs)
        ) {
            // Rhythm: instant page turn, session refreshed.
            lastRhythmEdgeMs = nowMs
            lastRhythmZone = zone
            lastRhythmEpoch = epoch.value
            onZoneTap(zone)
        } else {
            // Hold for a possible double-tap (single-tap-confirmed).
            // Center taps always take this path, so double-tap-to-zoom works
            // from any state. The hold stamps the detector epoch: a
            // direction flip mid-hold moves it, and the stale fire drops.
            val held = TapRecord(timeMs = nowMs, position = position, zone = zone)
            val heldEpoch = epoch.value
            pendingTap = held
            holdEpoch = heldEpoch
            holdJob?.cancel()
            holdJob = scope.launch {
                delay(DOUBLE_TAP_TIMEOUT_MS)
                if (pendingTap === held && epoch.value == heldEpoch) {
                    pendingTap = null
                    fireHeld(held, held.timeMs + DOUBLE_TAP_TIMEOUT_MS)
                }
            }
        }
    }

    try {
        awaitEachGesture {
        val downPositions = mutableMapOf<PointerId, Offset>()
        val downTimes = mutableMapOf<PointerId, Long>()
        // False when another finger was already down: overlapping contact
        // can never open a double-tap pair.
        val sequentialDowns = mutableSetOf<PointerId>()
        val currentPositions = mutableMapOf<PointerId, Offset>()
        // Worst drift per finger this gesture: a press that wandered past
        // the budget and came back is a gesture, never a tap.
        val maxDrifts = mutableMapOf<PointerId, Float>()
        // Rebase helper: narrow centered content resolves taps in viewport
        // space, so the same physical x always lands in the same zone.
        fun rebase(position: Offset): Triple<Offset, Offset, ReaderZone> {
            val widthPx = viewportWidth.value.coerceAtLeast(1f)
            val x = position.x + (widthPx - size.width) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val rebased = Offset(x, position.y)
            return Triple(rebased, center, zoneForTap((x / widthPx).coerceIn(0f, 1f), direction))
        }
        while (true) {
            val event = awaitPointerEvent()
            for (change in event.changes) {
                if (change.pressed) {
                    currentPositions[change.id] = change.position
                    if (!change.previousPressed) {
                        // A second finger voids an armed quick-scale: pinch
                        // takes over instead of fighting the hold-drag.
                        if (quickScale?.armed == true && change.id != quickScale.pointerId) {
                            quickScale.disarm()
                        }
                        if (downPositions.isEmpty()) {
                            sequentialDowns += change.id
                        }
                        downPositions[change.id] = change.position
                        downTimes[change.id] = change.uptimeMillis
                        // Second contact pairs a held tap-up into a zoom
                        // immediately (double-tap timing).
                        if (change.id in sequentialDowns) {
                            val (rebased, center, _) = rebase(change.position)
                            pairOnDown(change.id, rebased, center, change.uptimeMillis, change.isConsumed)
                        }
                    }
                }
            }
            // A press turning into a scroll or pinch abandons any held tap:
            // firing a page turn mid-gesture is worse than dropping it.
            val scrolling = currentPositions.any { (id, current) ->
                val start = downPositions[id] ?: current
                val drift = (current - start).getDistance()
                maxDrifts[id] = maxOf(maxDrifts[id] ?: 0f, drift)
                drift > engageSlop
            }
            if (scrolling) {
                cancelHold()
            }
            for (change in event.changes) {
                if (!change.pressed && change.previousPressed) {
                    val start = downPositions.remove(change.id)
                    val downMs = downTimes.remove(change.id)
                    val sequential = sequentialDowns.remove(change.id)
                    currentPositions.remove(change.id)
                    if (change.id in consumedIds) {
                        // Trailing up of a paired double-tap: swallowed, and
                        // consumed so outer detectors don't re-pair it into
                        // their own phantom toggle. A clean lift (no
                        // hold-drag) falls through to the stepped zoom here.
                        consumedIds -= change.id
                        val quick = quickScale
                        if (quick?.armed == true && quick.pointerId == change.id) {
                            val moved = quick.moved
                            quick.disarm()
                            if (!moved) {
                                val (rebased, center, _) = rebase(change.position)
                                onZoom(rebased, center)
                            }
                        }
                        if (consumeUp) {
                            change.consume()
                        }
                        continue
                    }
                    val ownDrift = start?.let { (change.position - it).getDistance() }
                    val stayedPut = (maxDrifts.remove(change.id) ?: 0f) <= engageSlop
                    val othersStill = downPositions.all { (id, startPos) ->
                        val current = currentPositions[id] ?: startPos
                        (current - startPos).getDistance() <= engageSlop
                    }
                    // Presses held past the long-press timeout are long
                    // presses, not taps — a two-second touch must never turn
                    // a page on release.
                    val quickTap = downMs?.let { isTapDurationValid(it, change.uptimeMillis, longPressTimeoutMs) } == true
                    if (ownDrift != null && ownDrift <= engageSlop && stayedPut && othersStill &&
                        quickTap && !change.isConsumed
                    ) {
                        if (consumeUp) {
                            change.consume()
                        }
                        val (rebased, center, zone) = rebase(change.position)
                        handleTap(
                            position = rebased,
                            center = center,
                            zone = zone,
                            nowMs = change.uptimeMillis,
                            canPair = sequential,
                        )
                    }
                }
            }
            if (event.changes.none { it.pressed }) break
        }
        }
    } finally {
        // Detector restarted or gone mid-pair: never strand an armed
        // hold-drag with no owner to finish it.
        quickScale?.disarm()
    }
}
