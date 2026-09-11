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
    fun unzoomedEdgeTapAlwaysTurns() {
        assertEquals(
            PanTurn.Turn,
            panOrTurn(scale = 1f, offsetX = 0f, viewportWidthPx = 1000f, towardTrailing = true),
        )
    }

    @Test
    fun zoomedEdgeTapPansFirst() {
        // scale 2.5 on 1000px: max pan 750; hop 400 toward trailing (negative).
        assertEquals(
            PanTurn.Pan(-400f),
            panOrTurn(scale = 2.5f, offsetX = 0f, viewportWidthPx = 1000f, towardTrailing = true),
        )
        // Leading tap pans the other way.
        assertEquals(
            PanTurn.Pan(400f),
            panOrTurn(scale = 2.5f, offsetX = 0f, viewportWidthPx = 1000f, towardTrailing = false),
        )
    }

    @Test
    fun edgeTapAtPanLimitTurns() {
        assertEquals(
            PanTurn.Turn,
            panOrTurn(scale = 2.5f, offsetX = -750f, viewportWidthPx = 1000f, towardTrailing = true),
        )
        assertEquals(
            PanTurn.Turn,
            panOrTurn(scale = 2.5f, offsetX = 750f, viewportWidthPx = 1000f, towardTrailing = false),
        )
    }

    @Test
    fun panClampsAtLimit() {
        // 600 + 400 would overshoot 750: clamps, still a pan.
        assertEquals(
            PanTurn.Pan(750f),
            panOrTurn(scale = 2.5f, offsetX = 600f, viewportWidthPx = 1000f, towardTrailing = false),
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
