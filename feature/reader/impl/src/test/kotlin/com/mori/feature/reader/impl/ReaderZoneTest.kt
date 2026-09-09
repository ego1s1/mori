package com.mori.feature.reader.impl

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import com.mori.core.model.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun volumeDownIsNextWhenEnabled() {
        assertEquals(
            ReaderAction.NextPage,
            volumeKeyAction(Key.VolumeDown, KeyEventType.KeyUp, volumeKeysEnabled = true),
        )
    }

    @Test
    fun volumeUpIsPrevWhenEnabled() {
        assertEquals(
            ReaderAction.PrevPage,
            volumeKeyAction(Key.VolumeUp, KeyEventType.KeyUp, volumeKeysEnabled = true),
        )
    }

    @Test
    fun volumeKeysIgnoredWhenDisabled() {
        assertNull(volumeKeyAction(Key.VolumeDown, KeyEventType.KeyUp, volumeKeysEnabled = false))
        assertNull(volumeKeyAction(Key.VolumeUp, KeyEventType.KeyUp, volumeKeysEnabled = false))
    }

    @Test
    fun volumeKeyDownEventsIgnored() {
        assertNull(volumeKeyAction(Key.VolumeDown, KeyEventType.KeyDown, volumeKeysEnabled = true))
    }

    @Test
    fun unrelatedKeysIgnored() {
        assertNull(volumeKeyAction(Key.DirectionRight, KeyEventType.KeyUp, volumeKeysEnabled = true))
    }
}
