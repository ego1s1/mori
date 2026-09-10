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
 * physical x always lands in the same zone. Edge taps (previous/next) dispatch
 * on tap-up with no double-tap wait, so rapid taps mid animation still turn
 * pages; only center taps hold the double-tap window ([decideTap]), where a
 * second center tap zooms and a lone one toggles chrome.
 *
 * Every finger is tracked independently inside one gesture: alternating
 * two-finger skipping dispatches one tap per finger-up, even with overlapping
 * contact. A press only counts when its own drift stays within touch slop, no
 * other finger drifted either (pinches void the whole gesture), and the up
 * wasn't claimed by a drag handler. Presses that drift past touch slop are
 * not taps and are ignored.
 *
 * [viewportWidth] is read live through state, so the detector loop (keyed on
 * [direction] only) survives rotation and resize without dropping in-flight
 * taps. When [consumeUp] is true the tap-up is consumed after handling, so an
 * outer detector further up the hit path ignores taps already claimed by a
 * page. Taps swallowed by chrome clickables never reach a detector at all.
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
    var pendingMenuTap: TapRecord? = null
    var menuJob: Job? = null

    fun handleTap(position: Offset, center: Offset, zone: ReaderZone) {
        menuJob?.cancel()
        menuJob = null
        when (
            decideTap(
                previous = pendingMenuTap,
                nowMs = SystemClock.uptimeMillis(),
                position = position,
                zone = zone,
                touchSlopPx = touchSlop,
            )
        ) {
            is TapDecision.Dispatch -> {
                pendingMenuTap = null
                onZoneTap(zone)
            }
            TapDecision.Zoom -> {
                pendingMenuTap = null
                onZoom(position, center)
            }
            TapDecision.AwaitSecondTap -> {
                pendingMenuTap = TapRecord(
                    timeMs = SystemClock.uptimeMillis(),
                    position = position,
                    zone = zone,
                )
                menuJob = scope.launch {
                    delay(DOUBLE_TAP_TIMEOUT_MS)
                    pendingMenuTap = null
                    onZoneTap(ReaderZone.MENU)
                }
            }
        }
    }

    awaitEachGesture {
        val downPositions = mutableMapOf<PointerId, Offset>()
        val currentPositions = mutableMapOf<PointerId, Offset>()
        while (true) {
            val event = awaitPointerEvent()
            for (change in event.changes) {
                if (change.pressed) {
                    currentPositions[change.id] = change.position
                    if (!change.previousPressed) {
                        downPositions[change.id] = change.position
                    }
                }
            }
            for (change in event.changes) {
                if (!change.pressed && change.previousPressed) {
                    val start = downPositions.remove(change.id)
                    currentPositions.remove(change.id)
                    val ownDrift = start?.let { (change.position - it).getDistance() }
                    val othersStill = downPositions.all { (id, startPos) ->
                        val current = currentPositions[id] ?: startPos
                        (current - startPos).getDistance() <= touchSlop
                    }
                    if (ownDrift != null && ownDrift <= touchSlop && othersStill && !change.isConsumed) {
                        if (consumeUp) {
                            change.consume()
                        }
                        // Rebase narrow centered content into viewport space.
                        val widthPx = viewportWidth.value.coerceAtLeast(1f)
                        val x = change.position.x + (widthPx - size.width) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val rebased = Offset(x, change.position.y)
                        handleTap(
                            position = rebased,
                            center = center,
                            zone = zoneForTap((x / widthPx).coerceIn(0f, 1f), direction),
                        )
                    }
                }
            }
            if (event.changes.none { it.pressed }) break
        }
    }
}
