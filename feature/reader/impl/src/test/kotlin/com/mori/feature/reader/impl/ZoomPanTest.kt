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
        // Below fit is still fit: never any offset.
        assertEquals(
            Offset.Zero,
            clampPan(Offset(100f, -50f), scale = 0.5f, widthPx = 400f, heightPx = 600f),
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

    @Test
    fun quickScaleMultiplierRejectsUnsetBaseline() {
        // No span sample yet (or a reset baseline): track only, never scale.
        assertEquals(1f, quickScaleMultiplier(200f, 0f, downwards = true))
        assertEquals(1f, quickScaleMultiplier(200f, -1f, downwards = true))
    }

    @Test
    fun quickScaleMultiplierTracksInsideThreshold() {
        // 3% span drift is below the 6% engage window: still tracking.
        assertEquals(1f, quickScaleMultiplier(103f, 100f, downwards = true))
        assertEquals(1f, quickScaleMultiplier(97f, 100f, downwards = false))
        // 7% drift engages: down-drag zooms in, up-drag zooms out.
        assertTrue(quickScaleMultiplier(107f, 100f, downwards = true) > 1f)
        assertTrue(quickScaleMultiplier(93f, 100f, downwards = false) < 1f)
    }

    @Test
    fun outwardPushRespectsEdgeTolerance() {
        // 2x on 400px: limits +-200, 1px tolerance.
        assertTrue(isOutwardPush(-199.5f, 2f, 400f, -10f))
        assertTrue(isOutwardPush(199.5f, 2f, 400f, 10f))
        // Just outside tolerance: still content, not an edge push.
        assertFalse(isOutwardPush(-198f, 2f, 400f, -10f))
        assertFalse(isOutwardPush(198f, 2f, 400f, 10f))
        // No travel means no push, even when clamped.
        assertFalse(isOutwardPush(-200f, 2f, 400f, 0f))
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
    fun flingTargetAtFitStaysCentered() {
        // Fit has no pan room: any release velocity still lands on center.
        assertEquals(
            Offset.Zero,
            flingTarget(Offset(10f, 20f), Offset(4_000f, 4_000f), scale = 1f, widthPx = 400f, heightPx = 600f),
        )
        assertEquals(
            Offset.Zero,
            flingTarget(Offset.Zero, Offset.Zero, scale = 0.5f, widthPx = 400f, heightPx = 600f),
        )
    }

    @Test
    fun slowReleasePassesThroughUncapped() {
        assertEquals(Offset(900f, -300f), capFlingVelocity(Offset(900f, -300f)))
    }

    @Test
    fun flingCapBoundaryPassesThrough() {
        // Zero stays zero; exactly at the ceiling keeps its vector.
        assertEquals(Offset.Zero, capFlingVelocity(Offset.Zero))
        assertEquals(Offset(12_000f, 0f), capFlingVelocity(Offset(12_000f, 0f)))
    }

    @Test
    fun violentSwipeCapsMagnitudeKeepsDirection() {
        val capped = capFlingVelocity(Offset(9_000f, 12_000f))
        // 15,000 px/s input clamps to the 12,000 ceiling, same heading.
        assertEquals(12_000f, capped.getDistance(), 0.5f)
        assertEquals(9_000f / 15_000f, capped.x / capped.getDistance(), 0.001f)
    }
}
