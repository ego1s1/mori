package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import com.mori.core.model.ReadingDirection

/**
 * Tap-zone outcome for a tap on a reader page.
 *
 * Inspired by the zone-navigation concept (thirds of the screen mapped to
 * previous/menu/next), restyled here around Material 3 Expressive chrome and our own
 * gesture stack: zones are resolved inside [ZoomablePage] so single taps navigate while
 * double-tap still zooms.
 */
enum class ReaderZone {
    PREV,
    MENU,
    NEXT,
}

/**
 * Resolves which zone a tap at horizontal [fraction] (`0f..1f` of page width) falls in.
 *
 * Outer thirds navigate; the center third toggles chrome. Mirrored for right-to-left
 * so "forward" always follows the reading direction.
 */
fun zoneForTap(fraction: Float, direction: ReadingDirection): ReaderZone {
    val forwardRight = direction == ReadingDirection.LEFT_TO_RIGHT
    return when {
        fraction < ZONE_EDGE -> if (forwardRight) ReaderZone.PREV else ReaderZone.NEXT
        fraction > 1f - ZONE_EDGE -> if (forwardRight) ReaderZone.NEXT else ReaderZone.PREV
        else -> ReaderZone.MENU
    }
}

private const val ZONE_EDGE = 1f / 3f

/** Outcome of an edge tap while zoomed: pan the art or turn the page. */
internal sealed interface PanTurn {
    /** Slide content horizontally to [targetOffsetX] (px, viewport space). */
    data class Pan(val targetOffsetX: Float) : PanTurn

    /** Already at the pan limit (or not zoomed): turn the page. */
    data object Turn : PanTurn
}

/**
 * Routes an edge tap while zoomed: pan toward the tapped side first, turn only
 * at the pan limit (reference-reader navigate-to-pan behavior).
 *
 * [towardTrailing] is true for taps on the screen's trailing (right) edge:
 * the NEXT zone in left-to-right, the PREV zone in right-to-left. Pan limits
 * assume width-fitted content (exact for [PageFit.WIDTH], approximate
 * otherwise): content overflows the viewport by `(scale - 1) * width`, half
 * each side, stepped in 40%-of-viewport hops.
 */
internal fun panOrTurn(
    scale: Float,
    offsetX: Float,
    viewportWidthPx: Float,
    towardTrailing: Boolean,
): PanTurn {
    if (scale <= 1f) return PanTurn.Turn
    val maxPan = viewportWidthPx * (scale - 1f) / 2f
    val step = viewportWidthPx * PAN_STEP_FRACTION * if (towardTrailing) -1f else 1f
    val target = (offsetX + step).coerceIn(-maxPan, maxPan)
    return if (target == offsetX) PanTurn.Turn else PanTurn.Pan(target)
}

private const val PAN_STEP_FRACTION = 0.4f

/**
 * Single-tap hold before a tap fires (single-tap-confirmed): the window a
 * second tap may still pair into a zoom. Shorter than AOSP's 300ms
 * `DOUBLE_TAP_TIMEOUT` so page turns feel snappy, but long enough that a
 * genuine double-tap's second contact (typically <200ms) always lands
 * inside it — pairing and the hold share this one constant, so the window
 * can never overlap inconsistently.
 */
internal const val DOUBLE_TAP_TIMEOUT_MS = 250L

/**
 * Pair-matching radius for a double-tap, as a multiple of touch slop. The
 * generous radius pairs a second tap that drifted (e.g. straddling the
 * center/edge boundary) back into one zoom instead of dealing a surprise
 * page turn.
 */
internal const val DOUBLE_TAP_SLOP_SCALE = 2f

/**
 * Rhythm window after a fired edge tap: further edge taps inside it dispatch
 * instantly for fast page skipping. Center taps always hold, so
 * double-tap-to-zoom works from any state.
 */
internal const val RHYTHM_TIMEOUT_MS = 1000L

/** A tap awaiting its double-tap window. */
internal data class TapRecord(
    val timeMs: Long,
    val position: Offset,
    val zone: ReaderZone,
)

/**
 * Whether the tap ending at [nowMs] pairs with a held [first] tap into one
 * double-tap: inside the timeout window and the pair radius. Sequentiality
 * (no overlapping contact) is tracked structurally by the detector, not here.
 * Pure for testability; all pairing math runs on pointer-event time.
 */
internal fun shouldPair(
    first: TapRecord?,
    nowMs: Long,
    position: Offset,
    touchSlopPx: Float,
    doubleTapTimeoutMs: Long = DOUBLE_TAP_TIMEOUT_MS,
): Boolean {
    if (first == null) return false
    if (nowMs - first.timeMs !in 0..doubleTapTimeoutMs) return false
    return (position - first.position).getDistance() <= touchSlopPx * DOUBLE_TAP_SLOP_SCALE
}

/**
 * Whether a press from [downMs] to [upMs] is a tap rather than a long press.
 * Pure for testability; defaults to the system long-press timeout.
 */
internal fun isTapDurationValid(
    downMs: Long,
    upMs: Long,
    longPressTimeoutMs: Long = android.view.ViewConfiguration.getLongPressTimeout().toLong(),
): Boolean = upMs - downMs in 0..longPressTimeoutMs

/**
 * Whether an edge tap at [nowMs] rides the instant-rhythm window opened by
 * [lastEdgeMs]. Pure for testability.
 */
internal fun isRhythmActive(
    lastEdgeMs: Long,
    nowMs: Long,
    rhythmTimeoutMs: Long = RHYTHM_TIMEOUT_MS,
): Boolean = lastEdgeMs != 0L && nowMs - lastEdgeMs in 0..rhythmTimeoutMs
