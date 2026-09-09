package com.mori.core.designsystem

import com.mori.core.model.MotionStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTest {

    @Test
    fun expressiveRunsUnlessSystemReducesMotion() {
        assertEquals(
            true,
            resolveExpressiveMotionEnabled(MotionStyle.EXPRESSIVE, systemReduceMotion = false),
        )
        assertEquals(
            false,
            resolveExpressiveMotionEnabled(MotionStyle.EXPRESSIVE, systemReduceMotion = true),
        )
    }

    @Test
    fun calmNeverRunsExpressiveMotion() {
        assertEquals(
            false,
            resolveExpressiveMotionEnabled(MotionStyle.CALM, systemReduceMotion = false),
        )
        assertEquals(
            false,
            resolveExpressiveMotionEnabled(MotionStyle.CALM, systemReduceMotion = true),
        )
    }

    @Test
    fun zeroAnimatorScaleMeansReduceMotion() {
        assertEquals(true, isSystemReduceMotionEnabled(0f))
        assertEquals(false, isSystemReduceMotionEnabled(1f))
        assertEquals(false, isSystemReduceMotionEnabled(0.5f))
    }
}
