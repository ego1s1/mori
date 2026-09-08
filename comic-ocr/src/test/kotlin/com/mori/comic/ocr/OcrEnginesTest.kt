package com.mori.comic.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OcrEnginesTest {

    @Test
    fun defaultIsFirstRegistered() {
        OcrEngines.clear()
        val a = FakeOcrEngine(id = "a")
        val b = FakeOcrEngine(id = "b")
        OcrEngines.register(a)
        OcrEngines.register(b)
        assertSame(a, OcrEngines.default())
        OcrEngines.clear()
    }

    @Test
    fun byIdReturnsMatchingEngine() {
        OcrEngines.clear()
        val a = FakeOcrEngine(id = "alpha")
        OcrEngines.register(a)
        assertSame(a, OcrEngines.byId("alpha"))
        assertNull(OcrEngines.byId("missing"))
        OcrEngines.clear()
    }

    @Test
    fun registerReplacesSameId() {
        OcrEngines.clear()
        OcrEngines.register(FakeOcrEngine(id = "x", displayName = "first"))
        OcrEngines.register(FakeOcrEngine(id = "x", displayName = "second"))
        val engines = OcrEngines.available()
        assertEquals(1, engines.size)
        assertEquals("second", engines.single().displayName)
        OcrEngines.clear()
    }

    @Test
    fun availableReturnsSnapshot() {
        OcrEngines.clear()
        OcrEngines.register(FakeOcrEngine(id = "one"))
        OcrEngines.register(FakeOcrEngine(id = "two"))
        val engines = OcrEngines.available()
        assertEquals(2, engines.size)
        assertEquals(listOf("one", "two"), engines.map { it.id })
        OcrEngines.clear()
    }

    @Test
    fun unregisterRemovesEngine() {
        OcrEngines.clear()
        val a = FakeOcrEngine(id = "a")
        OcrEngines.register(a)
        assertSame(a, OcrEngines.unregister("a"))
        assertNull(OcrEngines.byId("a"))
        OcrEngines.clear()
    }

    @Test
    fun clearReleasesEngines() {
        OcrEngines.clear()
        val engine = FakeOcrEngine(id = "a")
        OcrEngines.register(engine)
        OcrEngines.clear()
        assertTrue(OcrEngines.available().isEmpty())
        OcrEngines.clear()
    }

    @Test
    fun fakeEngineReportsLanguagesAndResults() = runTest {
        val engine = FakeOcrEngine(
            id = "f",
            availableLanguages = listOf("eng", "jpn"),
            results = mapOf(
                "eng" to OcrPageResult(
                    lines = listOf(OcrLine(listOf(OcrWord("hi", 0.9f, Rect(0, 0, 10, 10))), Rect(0, 0, 10, 10))),
                    text = "hi",
                    meanConfidence = 0.9f,
                    language = "eng",
                ),
            ),
        )
        assertEquals(listOf("eng", "jpn"), engine.supportedLanguages())
        val result = engine.recognize(bitmap(), "eng")
        assertEquals("hi", result.text)
        assertNotNull(result.lines.singleOrNull())
    }

    @Test
    fun releasedFakeEngineThrows() = runTest {
        val engine = FakeOcrEngine(id = "f")
        engine.release()
        assertThrows(OcrUnavailableException::class.java) {
            runBlocking { engine.recognize(bitmap(), "eng") }
        }
    }

    @Test
    fun fakeEngineCanInjectFailure() = runTest {
        val engine = FakeOcrEngine(id = "f", failWith = OcrLanguageException("nope"))
        assertThrows(OcrLanguageException::class.java) {
            runBlocking { engine.recognize(bitmap(), "eng") }
        }
    }

    private fun bitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}
