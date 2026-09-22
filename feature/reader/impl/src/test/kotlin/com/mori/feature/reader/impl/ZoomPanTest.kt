package com.mori.feature.reader.impl

import androidx.compose.ui.geometry.Offset
import com.mori.core.model.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPanTest {

    @Test
    fun fitScaleAlwaysRecenters() {
        assertEquals(
            Offset.Zero,
            clampPan(Offset(100f, -50f), scale = 1f, widthPx = 400f, heightPx = 600f),
        )
    }

    @Test
    fun panInsideBoundsPassesThrough() {
        assertEquals(
            Offset(100f, -50f),
            clampPan(Offset(100f, -50f), scale = 2f, widthPx = 400f, heightPx = 600f),
        )
    }

    @Test
    fun panClampsPerAxis() {
        // 2x on a 400x600 box: limits are +-200 x, +-300 y.
        assertEquals(
            Offset(200f, -300f),
            clampPan(Offset(500f, -900f), scale = 2f, widthPx = 400f, heightPx = 600f),
        )
    }

    @Test
    fun edgeTurnFollowsReadingDirection() {
        // Pushing past the left clamp advances in LTR, retreats in RTL.
        assertTrue(edgeTurnForward(-100f, ReadingDirection.LEFT_TO_RIGHT))
        assertFalse(edgeTurnForward(100f, ReadingDirection.LEFT_TO_RIGHT))
        assertTrue(edgeTurnForward(100f, ReadingDirection.RIGHT_TO_LEFT))
        assertFalse(edgeTurnForward(-100f, ReadingDirection.RIGHT_TO_LEFT))
    }

    @Test
    fun outwardPushOnlyAtClampedEdge() {
        // 2x on 400px: limits +-200.
        assertTrue(isOutwardPush(-200f, 2f, 400f, -10f))
        assertTrue(isOutwardPush(200f, 2f, 400f, 10f))
        // Pulling back into content is a pan, not a turn.
        assertFalse(isOutwardPush(-200f, 2f, 400f, 10f))
        assertFalse(isOutwardPush(200f, 2f, 400f, -10f))
        // Mid-content swipes never turn, even outward-moving.
        assertFalse(isOutwardPush(0f, 2f, 400f, -10f))
        assertFalse(isOutwardPush(0f, 2f, 400f, 10f))
        // Fit never turns.
        assertFalse(isOutwardPush(0f, 1f, 400f, -10f))
    }
}
