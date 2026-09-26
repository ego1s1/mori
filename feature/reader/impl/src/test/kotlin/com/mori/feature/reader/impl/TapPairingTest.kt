package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapPairingTest {

    @Test
    fun noHeldTapNeverPairs() {
        assertFalse(
            shouldPair(
                first = null,
                nowMs = 1_000L,
                position = Offset(500f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun sequentialSecondTapInWindowAndRadiusPairs() {
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertTrue(
            shouldPair(
                first = first,
                nowMs = 1_100L,
                position = Offset(505f, 505f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun driftedSecondTapInsideGenerousRadiusStillPairs() {
        // Straddling the center/edge boundary must zoom, not turn a page.
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertTrue(
            shouldPair(
                first = first,
                nowMs = 1_100L,
                position = Offset(500f + SLOP * 1.5f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun farSecondTapDoesNotPair() {
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertFalse(
            shouldPair(
                first = first,
                nowMs = 1_100L,
                position = Offset(900f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun expiredWindowDoesNotPair() {
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertFalse(
            shouldPair(
                first = first,
                nowMs = 1_000L + DOUBLE_TAP_TIMEOUT_MS + 1L,
                position = Offset(500f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun exactTimeoutBoundaryStillPairs() {
        // `in 0..timeout` is inclusive: the last millisecond still zooms.
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertTrue(
            shouldPair(
                first = first,
                nowMs = 1_000L + DOUBLE_TAP_TIMEOUT_MS,
                position = Offset(500f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun clockSkewNeverPairs() {
        // Event-time going backwards is skew, not a double-tap.
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertFalse(
            shouldPair(
                first = first,
                nowMs = 999L,
                position = Offset(500f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun exactRadiusBoundaryStillPairs() {
        // Pair radius is `<= slop * 2x`: a tap exactly on the rim zooms.
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertTrue(
            shouldPair(
                first = first,
                nowMs = 1_100L,
                position = Offset(500f + SLOP * DOUBLE_TAP_SLOP_SCALE, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun justOutsideRadiusDoesNotPair() {
        val first = TapRecord(timeMs = 1_000L, position = Offset(500f, 500f), zone = ReaderZone.MENU)
        assertFalse(
            shouldPair(
                first = first,
                nowMs = 1_100L,
                position = Offset(500f + SLOP * DOUBLE_TAP_SLOP_SCALE + 1f, 500f),
                touchSlopPx = SLOP,
            ),
        )
    }

    @Test
    fun rhythmActiveInsideWindow() {
        assertTrue(isRhythmActive(lastEdgeMs = 1_000L, nowMs = 1_500L))
    }

    @Test
    fun rhythmExpiredOutsideWindow() {
        assertFalse(isRhythmActive(lastEdgeMs = 1_000L, nowMs = 1_000L + RHYTHM_TIMEOUT_MS + 1L))
    }

    @Test
    fun rhythmAtExactTimeoutBoundaryActive() {
        // `in 0..timeout` is inclusive: the last millisecond still skips.
        assertTrue(isRhythmActive(lastEdgeMs = 1_000L, nowMs = 1_000L + RHYTHM_TIMEOUT_MS))
    }

    @Test
    fun rhythmBeforeEdgeNeverActive() {
        assertFalse(isRhythmActive(lastEdgeMs = 1_000L, nowMs = 999L))
    }

    @Test
    fun rhythmInactiveWithoutHistory() {
        assertFalse(isRhythmActive(lastEdgeMs = 0L, nowMs = 1_000L))
    }

    @Test
    fun quickPressIsTap() {
        assertTrue(isTapDurationValid(downMs = 1_000L, upMs = 1_200L, longPressTimeoutMs = 400L))
    }

    @Test
    fun heldPressIsNotTap() {
        assertFalse(isTapDurationValid(downMs = 1_000L, upMs = 1_600L, longPressTimeoutMs = 400L))
    }

    @Test
    fun exactLongPressBoundaryIsStillTap() {
        // `in 0..timeout` is inclusive: lifting exactly on the timeout taps.
        assertTrue(isTapDurationValid(downMs = 1_000L, upMs = 1_400L, longPressTimeoutMs = 400L))
    }

    @Test
    fun clockSkewIsNotTap() {
        assertFalse(isTapDurationValid(downMs = 1_000L, upMs = 900L, longPressTimeoutMs = 400L))
    }

    private companion object {
        const val SLOP = 16f
    }
}
