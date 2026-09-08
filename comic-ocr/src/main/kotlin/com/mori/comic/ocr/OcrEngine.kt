package com.mori.comic.ocr

import android.graphics.Bitmap

/**
 * A pluggable OCR engine.
 *
 * Implementations recognize text from a [Bitmap]. Engines are registered with [OcrEngines] so
 * consumers can discover and switch between backends (e.g. Tesseract, ML Kit, PaddleOCR)
 * without changing application code.
 */
interface OcrEngine {
    /** Stable identifier, e.g. `"tesseract"`. */
    val id: String

    /** Human-readable name, e.g. `"Tesseract (on-device)"`. */
    val displayName: String

    /** The language codes this engine can recognize, or an empty list when unknown. */
    fun supportedLanguages(): List<String>

    /**
     * Recognizes text in [bitmap].
     *
     * @param language a language code such as `"eng"`; behavior for unsupported codes is
     *   engine-defined (falls back to a default language).
     * @throws OcrUnavailableException when the engine has not been initialized or the
     *   required language model is missing.
     */
    suspend fun recognize(bitmap: Bitmap, language: String = defaultLanguage): OcrPageResult

    /** Releases engine resources. Safe to call more than once. */
    fun release()

    companion object {
        /** The language used when none is specified. */
        const val defaultLanguage: String = "eng"
    }
}
