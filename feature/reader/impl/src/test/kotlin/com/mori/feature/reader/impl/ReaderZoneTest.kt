package com.mori.feature.reader.impl

import com.mori.core.model.ReaderNavMode
import com.mori.core.model.ReadingDirection
import com.mori.core.model.TapInvertMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderZoneTest {

    @Test
    fun leftThirdIsPrevInLtr() {
        assertEquals(ReaderZone.PREV, zoneForTap(0f, ReadingDirection.LEFT_TO_RIGHT))
        assertEquals(ReaderZone.PREV, zoneForTap(0.3f, ReadingDirection.LEFT_TO_RIGHT))
    }

    @Test
    fun rightThirdIsNextInLtr() {
        assertEquals(ReaderZone.NEXT, zoneForTap(0.7f, ReadingDirection.LEFT_TO_RIGHT))
        assertEquals(ReaderZone.NEXT, zoneForTap(1f, ReadingDirection.LEFT_TO_RIGHT))
    }

    @Test
    fun centerIsMenu() {
        assertEquals(ReaderZone.MENU, zoneForTap(0.5f, ReadingDirection.LEFT_TO_RIGHT))
        assertEquals(ReaderZone.MENU, zoneForTap(0.5f, ReadingDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun zonesMirrorInRtl() {
        assertEquals(ReaderZone.NEXT, zoneForTap(0f, ReadingDirection.RIGHT_TO_LEFT))
        assertEquals(ReaderZone.PREV, zoneForTap(1f, ReadingDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun topMarginIsAlwaysMenu() {
        // Top 5% strip always brings up menu regardless of horizontal position or nav mode
        assertEquals(ReaderZone.MENU, zoneForTap(0.1f, 0.02f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.DEFAULT))
        assertEquals(ReaderZone.MENU, zoneForTap(0.9f, 0.02f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))
    }

    @Test
    fun lShapeNavigation() {
        // Top third -> PREV
        assertEquals(ReaderZone.PREV, zoneForTap(0.5f, 0.2f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))
        // Bottom third -> NEXT
        assertEquals(ReaderZone.NEXT, zoneForTap(0.5f, 0.8f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))
        // Middle left -> PREV
        assertEquals(ReaderZone.PREV, zoneForTap(0.2f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))
        // Middle center -> MENU
        assertEquals(ReaderZone.MENU, zoneForTap(0.5f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))
        // Middle right -> NEXT
        assertEquals(ReaderZone.NEXT, zoneForTap(0.8f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE))

        // RTL flips PREV and NEXT
        assertEquals(ReaderZone.NEXT, zoneForTap(0.5f, 0.2f, ReadingDirection.RIGHT_TO_LEFT, ReaderNavMode.L_SHAPE))
        assertEquals(ReaderZone.PREV, zoneForTap(0.5f, 0.8f, ReadingDirection.RIGHT_TO_LEFT, ReaderNavMode.L_SHAPE))
    }

    @Test
    fun kindlishNavigation() {
        // Top third -> MENU
        assertEquals(ReaderZone.MENU, zoneForTap(0.5f, 0.2f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.KINDLISH))
        // Bottom left third -> PREV
        assertEquals(ReaderZone.PREV, zoneForTap(0.2f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.KINDLISH))
        // Bottom right 2/3 -> NEXT
        assertEquals(ReaderZone.NEXT, zoneForTap(0.7f, 0.7f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.KINDLISH))
    }

    @Test
    fun edgeNavigation() {
        // Center cell -> MENU
        assertEquals(ReaderZone.MENU, zoneForTap(0.5f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.EDGE))
        // Bottom center -> PREV
        assertEquals(ReaderZone.PREV, zoneForTap(0.5f, 0.8f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.EDGE))
        // Edges (left, right, top-center) -> NEXT
        assertEquals(ReaderZone.NEXT, zoneForTap(0.1f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.EDGE))
        assertEquals(ReaderZone.NEXT, zoneForTap(0.9f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.EDGE))
        assertEquals(ReaderZone.NEXT, zoneForTap(0.5f, 0.2f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.EDGE))
    }

    @Test
    fun rightAndLeftNavigation() {
        // Left third is always Left (PREV in LTR, NEXT in RTL)
        assertEquals(
            ReaderZone.PREV,
            zoneForTap(0.2f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.RIGHT_AND_LEFT)
        )
        assertEquals(
            ReaderZone.NEXT,
            zoneForTap(0.2f, 0.5f, ReadingDirection.RIGHT_TO_LEFT, ReaderNavMode.RIGHT_AND_LEFT)
        )
        // Right third is always Right (NEXT in LTR, PREV in RTL)
        assertEquals(
            ReaderZone.NEXT,
            zoneForTap(0.8f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.RIGHT_AND_LEFT)
        )
        assertEquals(
            ReaderZone.PREV,
            zoneForTap(0.8f, 0.5f, ReadingDirection.RIGHT_TO_LEFT, ReaderNavMode.RIGHT_AND_LEFT)
        )
        // Center third is MENU
        assertEquals(
            ReaderZone.MENU,
            zoneForTap(0.5f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.RIGHT_AND_LEFT)
        )
    }

    @Test
    fun disabledNavigation() {
        assertEquals(ReaderZone.MENU, zoneForTap(0.1f, 0.1f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.DISABLED))
        assertEquals(ReaderZone.MENU, zoneForTap(0.9f, 0.9f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.DISABLED))
    }

    @Test
    fun tapInversion() {
        // Horizontal invert flips left/right in DEFAULT mode
        assertEquals(
            ReaderZone.NEXT,
            zoneForTap(0.2f, 0.5f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.DEFAULT, TapInvertMode.HORIZONTAL),
        )
        // Vertical invert flips top/bottom in L_SHAPE mode
        // In L_SHAPE, normal top (y=0.2) is PREV. With vertical invert, top becomes bottom (NEXT)
        assertEquals(
            ReaderZone.NEXT,
            zoneForTap(0.5f, 0.2f, ReadingDirection.LEFT_TO_RIGHT, ReaderNavMode.L_SHAPE, TapInvertMode.VERTICAL),
        )
    }
}
