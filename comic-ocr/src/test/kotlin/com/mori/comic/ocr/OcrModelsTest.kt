package com.mori.comic.ocr

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrModelsTest {

    @Test
    fun lineTextJoinsWordsWithSpaces() {
        val line = OcrLine(
            words = listOf(
                OcrWord("The", 0.9f, Rect(0, 0, 10, 10)),
                OcrWord("quick", 0.8f, Rect(12, 0, 30, 10)),
                OcrWord("fox", 0.7f, Rect(32, 0, 40, 10)),
            ),
            boundingBox = Rect(0, 0, 40, 10),
        )
        assertEquals("The quick fox", line.text)
    }

    @Test
    fun emptyPageResultIsEmpty() {
        assertTrue(OcrPageResult(emptyList(), "", 0f, "eng").isEmpty)
        assertTrue(OcrPageResult(emptyList(), "   ", 0f, "eng").isEmpty)
    }

    @Test
    fun nonEmptyPageResultIsNotEmpty() {
        val line = OcrLine(listOf(OcrWord("word", 1f, Rect(0, 0, 5, 5))), Rect(0, 0, 5, 5))
        assertFalse(OcrPageResult(listOf(line), "word", 1f, "eng").isEmpty)
    }

    @Test
    fun lineTextIsEmptyForNoWords() {
        assertEquals("", OcrLine(emptyList(), Rect(0, 0, 0, 0)).text)
    }
}
