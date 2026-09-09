package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class TapDecisionTest {

    @Test
    fun edgeTapDispatchesImmediatelyWithNoHistory() {
        assertEquals(
            TapDecision.Dispatch(ReaderZone.NEXT),
            decideTap(
                previous = null,
                nowMs = 1_000L,
                position = Offset(900f, 500f),
                zone = ReaderZone.NEXT,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun edgeTapDispatchesImmediatelyDespitePendingMenuTap() {
        // Rapid skipping: a second edge tap inside the double-tap window still turns
        // the page instead of waiting or zooming.
        val pending = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertEquals(
            TapDecision.Dispatch(ReaderZone.NEXT),
            decideTap(
                previous = pending,
                nowMs = 1_100L,
                position = Offset(500f, 500f),
                zone = ReaderZone.NEXT,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun loneCenterTapWaitsForSecondTap() {
        assertEquals(
            TapDecision.AwaitSecondTap,
            decideTap(
                previous = null,
                nowMs = 1_000L,
                position = Offset(500f, 500f),
                zone = ReaderZone.MENU,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun secondCenterTapInWindowZooms() {
        val pending = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertEquals(
            TapDecision.Zoom,
            decideTap(
                previous = pending,
                nowMs = 1_000L + DOUBLE_TAP_TIMEOUT_MS,
                position = Offset(500f, 500f),
                zone = ReaderZone.MENU,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun centerTapPastTimeoutWaitsAgain() {
        val pending = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertEquals(
            TapDecision.AwaitSecondTap,
            decideTap(
                previous = pending,
                nowMs = 1_000L + DOUBLE_TAP_TIMEOUT_MS + 1L,
                position = Offset(500f, 500f),
                zone = ReaderZone.MENU,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun centerTapBeyondSlopWaitsAgain() {
        val pending = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertEquals(
            TapDecision.AwaitSecondTap,
            decideTap(
                previous = pending,
                nowMs = 1_100L,
                position = Offset(500f + SLOP + 1f, 500f),
                zone = ReaderZone.MENU,
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun centerTapAfterEdgeTapWaits() {
        val pending = TapRecord(timeMs = 1_000L, position = Offset(900f, 500f), zone = ReaderZone.NEXT)
        assertEquals(
            TapDecision.AwaitSecondTap,
            decideTap(
                previous = pending,
                nowMs = 1_100L,
                position = Offset(500f, 500f),
                zone = ReaderZone.MENU,
                touchSlopPx = SLOP,
            ),
        )
    }

    private companion object {
        const val SLOP = 24f
    }
}
