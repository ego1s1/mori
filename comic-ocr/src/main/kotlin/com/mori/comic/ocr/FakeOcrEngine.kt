package com.mori.comic.ocr

import android.graphics.Bitmap

/**
 * A deterministic, in-memory [OcrEngine] for tests and as a reference implementation.
 *
 * It returns a fixed script of results configured at construction; useful both in unit tests
 * of the registry/abstraction and as a sample for implementers of a new backend.
 */
class FakeOcrEngine(
    override val id: String = "fake",
    override val displayName: String = "Fake OCR",
    private val availableLanguages: List<String> = listOf(OcrEngine.defaultLanguage),
    private val results: Map<String, OcrPageResult> = emptyMap(),
    private val failWith: OcrException? = null,
) : OcrEngine {

    private var released = false

    override fun supportedLanguages(): List<String> = availableLanguages

    override suspend fun recognize(bitmap: Bitmap, language: String): OcrPageResult {
        if (released) throw OcrUnavailableException("Fake engine released")
        failWith?.let { throw it }
        return results[language] ?: OcrPageResult(emptyList(), "", 0f, language)
    }

    override fun release() {
        released = true
    }
}
