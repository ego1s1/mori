package com.mori.feature.reader.impl

import android.os.SystemClock
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
 * Mihon-parity dispatch: every tap holds a small double-tap window before it
 * fires (single-tap-confirmed). A second contact landing in-window pairs into
 * a zoom immediately — on second down, like Mihon's `onDoubleTap` — so pairs
 * never dispatch and there is nothing to undo or compensate. Held edge taps
 * open a short rhythm window of instant turns for fast skipping; center taps
 * always hold, so double-tap-to-zoom works from any state; a press that drifts
 * into a scroll or pinch stands the hold down silently.
 *
 * Every finger is tracked independently inside one gesture: alternating
 * two-finger skipping dispatches one tap per finger-up, even with overlapping
 * contact (overlapping downs never pair — pairs are strictly sequential).
 * A press only counts when its own drift stays within touch slop, no other
 * finger drifted either (pinches void the whole gesture), and the up wasn't
 * claimed by a drag handler. Presses that drift past touch slop are not taps
 * and are ignored. The trailing up of a paired double-tap is swallowed.
 *
 * All pairing math runs on pointer-event time, so tests drive it with the
 * test clock. [viewportWidth] is read live through state, so the detector
 * loop (keyed on [direction] only) survives rotation and resize without
 * dropping in-flight taps. When [consumeUp] is true the tap-up is consumed
 * after handling, so an outer detector further up the hit path ignores taps
 * already claimed by a page. Taps swallowed by chrome clickables never reach
 * a detector at all.
 */
internal fun Modifier.zoneTaps(
    viewportWidth: State<Float>,
    direction: ReadingDirection,
    scope: CoroutineScope,
    onZoneTap: (ReaderZone) -> Unit,
    onZoom: (tap: Offset, center: Offset) -> Unit,
    consumeUp: Boolean,
): Modifier = pointerInput(direction) {
    val touchSlop = viewConfiguration.touchSlop
    var pendingTap: TapRecord? = null
    var holdJob: Job? = null
    var lastRhythmEdgeMs = 0L
    val consumedIds = mutableSetOf<PointerId>()

    fun cancelHold() {
        holdJob?.cancel()
        holdJob = null
        pendingTap = null
    }

    /** Fires a held tap; edge taps open the instant-rhythm window. */
    fun fireHeld(tap: TapRecord) {
        if (tap.zone != ReaderZone.MENU) {
            lastRhythmEdgeMs = tap.timeMs
        }
        onZoneTap(tap.zone)
    }

    /**
     * Pairs a fresh down with a held tap-up (Mihon `onDoubleTap` timing):
     * the zoom fires on second contact, so the pair never dispatches and the
     * trailing up is swallowed via [consumedIds].
     */
    fun pairOnDown(id: PointerId, position: Offset, center: Offset, downMs: Long): Boolean {
        val armed = pendingTap ?: return false
        if (!shouldPair(armed, downMs, position, touchSlop)) {
            return false
        }
        cancelHold()
        consumedIds += id
        onZoom(position, center)
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
        if (canPair && shouldPair(armed, nowMs, position, touchSlop)) {
            // Double-tap completed on lift (the down already paired when the
            // frames arrived separately): zoom, and neither tap dispatches —
            // the first was held, so there is nothing to undo.
            cancelHold()
            onZoom(position, center)
            return
        }
        if (armed != null) {
            // Early confirm: a far or overlapping second tap rules out
            // pairing, so fire the held tap now instead of waiting out the
            // window.
            cancelHold()
            fireHeld(armed)
        }
        if (zone == ReaderZone.MENU || !isRhythmActive(lastRhythmEdgeMs, nowMs)) {
            // Hold for a possible double-tap (Mihon single-tap-confirmed).
            // Center taps always take this path, so double-tap-to-zoom works
            // from any state.
            val held = TapRecord(timeMs = nowMs, position = position, zone = zone)
            pendingTap = held
            holdJob?.cancel()
            holdJob = scope.launch {
                delay(DOUBLE_TAP_TIMEOUT_MS)
                if (pendingTap?.timeMs == held.timeMs) {
                    pendingTap = null
                    fireHeld(held)
                }
            }
        } else {
            // Rhythm: instant page turn, session refreshed.
            lastRhythmEdgeMs = nowMs
            onZoneTap(zone)
        }
    }

    awaitEachGesture {
        val downPositions = mutableMapOf<PointerId, Offset>()
        val downTimes = mutableMapOf<PointerId, Long>()
        // False when another finger was already down: overlapping contact
        // can never open a double-tap pair (system-detector parity).
        val sequentialDowns = mutableSetOf<PointerId>()
        val currentPositions = mutableMapOf<PointerId, Offset>()
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
                        if (downPositions.isEmpty()) {
                            sequentialDowns += change.id
                        }
                        downPositions[change.id] = change.position
                        downTimes[change.id] = change.uptimeMillis
                        // Second contact pairs a held tap-up into a zoom
                        // immediately (Mihon onDoubleTap timing).
                        if (change.id in sequentialDowns) {
                            val (rebased, center, _) = rebase(change.position)
                            pairOnDown(change.id, rebased, center, change.uptimeMillis)
                        }
                    }
                }
            }
            // A press turning into a scroll or pinch abandons any held tap:
            // firing a page turn mid-gesture is worse than dropping it.
            val scrolling = currentPositions.any { (id, current) ->
                val start = downPositions[id] ?: current
                (current - start).getDistance() > touchSlop
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
                        // their own phantom toggle.
                        consumedIds -= change.id
                        if (consumeUp) {
                            change.consume()
                        }
                        continue
                    }
                    val ownDrift = start?.let { (change.position - it).getDistance() }
                    val othersStill = downPositions.all { (id, startPos) ->
                        val current = currentPositions[id] ?: startPos
                        (current - startPos).getDistance() <= touchSlop
                    }
                    // Presses held past the long-press timeout are long
                    // presses, not taps (Mihon/AOSP parity) — a two-second
                    // touch must never turn a page on release.
                    val quickTap = downMs?.let { isTapDurationValid(it, change.uptimeMillis) } == true
                    if (ownDrift != null && ownDrift <= touchSlop && othersStill &&
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
}
