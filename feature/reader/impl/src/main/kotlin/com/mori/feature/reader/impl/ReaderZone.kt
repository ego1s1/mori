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

/**
 * Standard Android double-tap timeout (AOSP `DOUBLE_TAP_TIMEOUT`). The page's tap
 * state machine holds a center tap for this long awaiting a second tap; edge taps
 * dispatch immediately so rapid page skipping never waits on animation or timeout.
 */
internal const val DOUBLE_TAP_TIMEOUT_MS = 300L

/** A tap awaiting its double-tap window. */
internal data class TapRecord(
    val timeMs: Long,
    val position: Offset,
    val zone: ReaderZone,
)

/** Outcome of feeding one tap-up through [decideTap]. */
internal sealed interface TapDecision {
    /** Dispatch now (page turn, or chrome toggle when no zoom is possible). */
    data class Dispatch(val zone: ReaderZone) : TapDecision

    /** Center tap: hold chrome and wait out the double-tap window. */
    data object AwaitSecondTap : TapDecision

    /** Second center tap in-window: zoom instead of toggling chrome. */
    data object Zoom : TapDecision
}

/**
 * Routes a tap-up without ever delaying edge navigation.
 *
 * Only a center tap following another center tap — close in time (within
 * [doubleTapTimeoutMs]) and space (within [touchSlopPx]) — becomes [TapDecision.Zoom].
 * Everything on the outer thirds dispatches immediately, even back-to-back mid
 * page-turn animation, so skipping pages fast feels instant.
 */
internal fun decideTap(
    previous: TapRecord?,
    nowMs: Long,
    position: Offset,
    zone: ReaderZone,
    doubleTapTimeoutMs: Long = DOUBLE_TAP_TIMEOUT_MS,
    touchSlopPx: Float,
): TapDecision {
    if (zone == ReaderZone.MENU && previous?.zone == ReaderZone.MENU &&
        nowMs - previous.timeMs in 0..doubleTapTimeoutMs &&
        (position - previous.position).getDistance() <= touchSlopPx
    ) {
        return TapDecision.Zoom
    }
    return if (zone == ReaderZone.MENU) TapDecision.AwaitSecondTap else TapDecision.Dispatch(zone)
}
