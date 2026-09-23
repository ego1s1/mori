package com.mori.feature.reader.impl

import com.mori.core.model.DisplayFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayFilterRenderTest {

    @Test
    fun neutralFilterRendersNothing() {
        assertNull(colorMatrixFor(DisplayFilter.Neutral))
        assertEquals(0f, dimAlphaFor(0f), 0f)
        assertEquals(0f, liftAlphaFor(0f), 0f)
        assertEquals(0f, nightAlphaFor(0f), 0f)
    }

    @Test
    fun grayscaleMatrixDesaturates() {
        val matrix = colorMatrixFor(DisplayFilter(grayscale = true))
        assertNotNull(matrix)
        val values = matrix!!.values
        // Luminance rows: R+G+B weights sum to 1, alpha untouched.
        for (row in 0..2) {
            val sum = values[row * 5] + values[row * 5 + 1] + values[row * 5 + 2]
            assertEquals(1f, sum, 0.01f)
            assertEquals(0f, values[row * 5 + 4], 0.01f)
        }
        assertEquals(listOf(0f, 0f, 0f, 1f, 0f), values.slice(15..19))
    }

    @Test
    fun invertMatrixNegatesWithOffset() {
        val matrix = colorMatrixFor(DisplayFilter(invert = true))
        assertNotNull(matrix)
        val values = matrix!!.values
        assertEquals(
            listOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
            values.toList(),
        )
    }

    @Test
    fun combinedMatrixGrayscalesThenInverts() {
        // Pure red through gray-then-invert: luminance ~54, inverted ~201.
        val matrix = colorMatrixFor(DisplayFilter(grayscale = true, invert = true))
        assertNotNull(matrix)
        val v = matrix!!.values
        val redOut = v[0] * 255f + v[4]
        assertTrue(redOut > 190f && redOut < 215f)
        // Alpha channel untouched.
        assertEquals(listOf(0f, 0f, 0f, 1f, 0f), v.slice(15..19))
    }

    @Test
    fun brightnessMapsToOverlays() {
        assertEquals(MAX_DIM_ALPHA, dimAlphaFor(-1f), 0.01f)
        assertEquals(0f, dimAlphaFor(0.5f), 0f)
        assertEquals(0f, liftAlphaFor(-0.5f), 0f)
        assertEquals(MAX_LIFT_ALPHA, liftAlphaFor(1f), 0.01f)
    }

    @Test
    fun nightTintScales() {
        assertEquals(0f, nightAlphaFor(0f), 0f)
        assertEquals(MAX_NIGHT_ALPHA, nightAlphaFor(1f), 0.01f)
        assertEquals(MAX_NIGHT_ALPHA / 2f, nightAlphaFor(0.5f), 0.01f)
    }

    @Test
    fun coerceClampsChannels() {
        val coerced = DisplayFilter(brightness = 4f, nightTint = -2f).coerce()
        assertEquals(1f, coerced.brightness, 0f)
        assertEquals(0f, coerced.nightTint, 0f)
    }
}
