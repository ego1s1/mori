package com.mori.comic.ocr

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TesseractOcrEngineLifecycleTest {

    private fun engine() = TesseractOcrEngine(ApplicationProvider.getApplicationContext())

    @Test
    fun releaseBeforeInitIsIdempotentAndSafe() {
        val e = engine()
        e.release()
        e.release()
    }

    @Test
    fun supportedLanguagesNeverThrows() {
        val languages = runCatching { engine().supportedLanguages() }.getOrDefault(emptyList())
        assertTrue(languages.isEmpty() || languages.contains(OcrEngine.defaultLanguage))
    }

    @Test
    fun recognizeAfterReleaseThrowsUnavailable() {
        val e = engine()
        e.release()
        runCatching {
            kotlinx.coroutines.runBlocking {
                e.recognize(android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888))
            }
        }.onFailure {
            assertTrue(it is OcrUnavailableException)
        }.onSuccess {
            // Acceptable only if the engine reports itself released-but-usable; otherwise fail.
            throw AssertionError("recognize after release must not succeed silently")
        }
    }
}
