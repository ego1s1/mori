package com.mori.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class PageFitAspectTest {

    @Test
    fun aspectFollowsDecodedDims() {
        assertEquals(0.5f, artAspectFor(800f, 1600f), 0.0001f)
        assertEquals(1.5f, artAspectFor(1200f, 800f), 0.0001f)
    }

    @Test
    fun degenerateSizesFallBackToPlaceholder() {
        assertEquals(2f / 3f, artAspectFor(0f, 800f), 0.0001f)
        assertEquals(2f / 3f, artAspectFor(800f, 0f), 0.0001f)
        assertEquals(2f / 3f, artAspectFor(-4f, 800f), 0.0001f)
    }

    @Test
    fun extremesAreClamped() {
        assertEquals(5f, artAspectFor(10000f, 1f), 0.0001f)
        assertEquals(0.2f, artAspectFor(1f, 10000f), 0.0001f)
    }
}
