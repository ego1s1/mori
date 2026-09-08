package com.mori.comic.ocr

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TesseractOcrEngineTest {

    @Test
    fun groupIntoSingleLineForWordsOnSameBaseline() {
        val words = listOf(
            word("hello", 0, 0, 40, 20),
            word("world", 50, 0, 100, 20),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        assertEquals(1, lines.size)
        assertEquals("hello world", lines[0].text)
        assertEquals(listOf("hello", "world"), lines[0].words.map { it.text })
    }

    @Test
    fun groupIntoSeparateLinesForDifferentBaselines() {
        val words = listOf(
            word("top", 0, 0, 40, 20),
            word("bottom", 0, 100, 60, 120),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        assertEquals(2, lines.size)
        assertEquals("top", lines[0].text)
        assertEquals("bottom", lines[1].text)
    }

    @Test
    fun wordsWithinToleranceShareLine() {
        // Second word is offset by half its height (within 0.6 tolerance).
        val words = listOf(
            word("a", 0, 0, 20, 20),
            word("b", 30, 10, 50, 30),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        assertEquals(1, lines.size)
    }

    @Test
    fun wordsOutOfToleranceSplitLines() {
        val words = listOf(
            word("a", 0, 0, 20, 20),
            word("b", 30, 40, 50, 60),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        assertEquals(2, lines.size)
    }

    @Test
    fun sortsWithinLineLeftToRight() {
        val words = listOf(
            word("right", 50, 0, 100, 20),
            word("left", 0, 0, 40, 20),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        assertEquals(listOf("left", "right"), lines.single().words.map { it.text })
    }

    @Test
    fun emptyWordsYieldEmptyLines() {
        assertTrue(TesseractOcrEngine.groupIntoLines(emptyList()).isEmpty())
    }

    @Test
    fun lineBoundingBoxSpansAllWords() {
        val words = listOf(
            word("a", 0, 0, 20, 20),
            word("b", 30, 0, 80, 20),
        )
        val line = TesseractOcrEngine.groupIntoLines(words).single()
        assertEquals(0, line.boundingBox.left)
        assertEquals(80, line.boundingBox.right)
        assertEquals(0, line.boundingBox.top)
        assertEquals(20, line.boundingBox.bottom)
    }

    @Test
    fun zeroHeightWordsDoNotDivideByZero() {
        val words = listOf(
            word("a", 0, 0, 0, 0),
            word("b", 10, 0, 10, 0),
        )
        val lines = TesseractOcrEngine.groupIntoLines(words)
        // Does not throw; grouping is deterministic (same synthetic baseline).
        assertEquals(1, lines.size)
    }

    private fun word(text: String, l: Int, t: Int, r: Int, b: Int) =
        OcrWord(text, 1.0f, Rect(l, t, r, b))
}
