package com.mori.feature.reader.impl

import com.mori.core.model.ReadingDirection
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
}
