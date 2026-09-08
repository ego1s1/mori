package com.mori.comic.decode

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeCalculatorTest {

    @Test
    fun noLimitReturnsOne() {
        assertEquals(1, SampleSizeCalculator.forDimension(4000, 3000, 0))
        assertEquals(1, SampleSizeCalculator.forDimension(4000, 3000, -1))
    }

    @Test
    fun fitsAlreadyReturnsOne() {
        assertEquals(1, SampleSizeCalculator.forDimension(400, 300, 500))
        assertEquals(1, SampleSizeCalculator.forDimension(500, 500, 500))
    }

    @Test
    fun doublesUntilBounded() {
        assertEquals(2, SampleSizeCalculator.forDimension(1200, 923, 600))
        assertEquals(4, SampleSizeCalculator.forDimension(1200, 923, 500))
        assertEquals(4, SampleSizeCalculator.forDimension(1200, 923, 300))
        assertEquals(8, SampleSizeCalculator.forDimension(1200, 923, 200))
        assertEquals(8, SampleSizeCalculator.forDimension(1200, 923, 150))
        assertEquals(16, SampleSizeCalculator.forDimension(1200, 923, 149))
    }

    @Test
    fun longestSideDrivesDecision() {
        // Tall narrow: height dominates.
        assertEquals(2, SampleSizeCalculator.forDimension(400, 1200, 600))
        // Wide short: width dominates.
        assertEquals(2, SampleSizeCalculator.forDimension(1200, 400, 600))
    }

    @Test
    fun boundaryExactlyAtMaxDimension() {
        // longest == maxDimension means already fits -> 1.
        assertEquals(1, SampleSizeCalculator.forDimension(1000, 1000, 1000))
        // one above -> 2.
        assertEquals(2, SampleSizeCalculator.forDimension(1001, 1000, 1000))
    }

    @Test
    fun roundToPowerOfTwo() {
        assertEquals(1, SampleSizeCalculator.roundToPowerOfTwo(1))
        assertEquals(1, SampleSizeCalculator.roundToPowerOfTwo(0))
        assertEquals(1, SampleSizeCalculator.roundToPowerOfTwo(-5))
        assertEquals(2, SampleSizeCalculator.roundToPowerOfTwo(2))
        assertEquals(2, SampleSizeCalculator.roundToPowerOfTwo(3))
        assertEquals(4, SampleSizeCalculator.roundToPowerOfTwo(4))
        assertEquals(4, SampleSizeCalculator.roundToPowerOfTwo(5))
        assertEquals(4, SampleSizeCalculator.roundToPowerOfTwo(7))
        assertEquals(8, SampleSizeCalculator.roundToPowerOfTwo(8))
        assertEquals(8, SampleSizeCalculator.roundToPowerOfTwo(15))
        assertEquals(16, SampleSizeCalculator.roundToPowerOfTwo(16))
    }

    @Test
    fun roundToPowerOfTwoClampsToMax() {
        assertEquals(1 shl 15, SampleSizeCalculator.roundToPowerOfTwo(Int.MAX_VALUE))
        assertEquals(1 shl 15, SampleSizeCalculator.roundToPowerOfTwo(1 shl 20))
        assertEquals(1 shl 15, SampleSizeCalculator.forDimension(Int.MAX_VALUE, Int.MAX_VALUE, 1))
    }

    @Test
    fun tinyMaxDimensionOnZeroSize() {
        assertEquals(1, SampleSizeCalculator.forDimension(0, 0, 1))
        assertEquals(1, SampleSizeCalculator.forDimension(0, 0, 500))
    }
}
