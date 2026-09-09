package com.mori.feature.reader.impl

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
