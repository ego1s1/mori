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
    fun edgeOvershootGrowsOnlyOnOutwardHorizontalPush() {
        // Pinned at +200 pushing right accumulates; 2x on 400px box.
        assertEquals(
            30f,
            accumulateEdgeOvershoot(20f, excessX = 10f, clampedX = 200f, horizontalPush = true),
            0.001f,
        )
        // Pulling back in resets even while still clamped.
        assertEquals(
            0f,
            accumulateEdgeOvershoot(20f, excessX = -10f, clampedX = 200f, horizontalPush = true),
            0.001f,
        )
        // Vertical drift resets.
        assertEquals(
            0f,
            accumulateEdgeOvershoot(20f, excessX = 10f, clampedX = 200f, horizontalPush = false),
            0.001f,
        )
        // Centered content never turns.
        assertEquals(
            0f,
            accumulateEdgeOvershoot(20f, excessX = 10f, clampedX = 0f, horizontalPush = true),
            0.001f,
        )
    }

    @Test
    fun flingTargetProjectsVelocityThenClamps() {
        // Quarter-second of velocity inside bounds passes through.
        assertEquals(
            Offset(100f, -50f),
            flingTarget(Offset.Zero, Offset(400f, -200f), scale = 2f, widthPx = 400f, heightPx = 600f),
        )
        // 2x on a 400x600 box: limits +-200 x, +-300 y.
        assertEquals(
            Offset(200f, 300f),
            flingTarget(Offset.Zero, Offset(4_000f, 4_000f), scale = 2f, widthPx = 400f, heightPx = 600f),
        )
    }

    @Test
    fun slowReleasePassesThroughUncapped() {
        assertEquals(Offset(900f, -300f), capFlingVelocity(Offset(900f, -300f)))
    }

    @Test
    fun violentSwipeCapsMagnitudeKeepsDirection() {
        val capped = capFlingVelocity(Offset(9_000f, 12_000f))
        // 15,000 px/s input clamps to the 12,000 ceiling, same heading.
        assertEquals(12_000f, capped.getDistance(), 0.5f)
        assertEquals(9_000f / 15_000f, capped.x / capped.getDistance(), 0.001f)
    }
}
