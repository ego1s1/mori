package com.mori.comic.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import dev.ffmpegkit.tesseract.TesseractConfig
import dev.ffmpegkit.tesseract.TesseractException
import dev.ffmpegkit.tesseract.TesseractOCR
import dev.ffmpegkit.tesseract.TesseractResult
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * [OcrEngine] backed by Tesseract 5 (on-device, offline) via the prebuilt
 * `dev.ffmpegkit-maintained:tesseract-android` AAR.
 *
 * The engine is initialized lazily on first use with [defaultLanguage]. Calls are safe to
 * make from any thread; recognition runs off the main thread (the underlying engine uses
 * coroutines).
 *
 * @param context application context, used to locate managed language data.
 * @param config optional [TesseractConfig]; defaults to the engine's automatic configuration.
 */
class TesseractOcrEngine(
    private val context: Context,
    private val config: TesseractConfig = TesseractConfig(),
) : OcrEngine {

    private val engine = TesseractOCR()
    private val lock = Any()
    private var initializedLanguage: String? = null
    private var released = false

    override val id: String = ID

    override val displayName: String = "Tesseract (on-device)"

    override fun supportedLanguages(): List<String> = runCatching {
        engine.getAvailableLanguages(context)
    }.getOrDefault(emptyList())

    override suspend fun recognize(bitmap: Bitmap, language: String): OcrPageResult {
        ensureInitialized(language)
        val result = runRecognition(bitmap)
        return toPageResult(result, language)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runRecognition(bitmap: Bitmap): TesseractResult {
        try {
            return engine.recognize(bitmap)
        } catch (e: TesseractException) {
            throw mapException(e)
        } catch (e: RuntimeException) {
            throw toRecognitionFailure(e)
        }
    }

    /**
     * Installs a `*.traineddata` file for [language] from [file]. Once added, the language
     * becomes usable in [recognize].
     */
    suspend fun addLanguage(language: String, file: File) {
        ensureNotReleased()
        runCatching { engine.addLanguage(context, language, file) }
            .onFailure { throw OcrLanguageException("Failed to add language '$language'", it) }
    }

    override fun release() {
        synchronized(lock) {
            if (released) return
            released = true
        }
        runCatching { engine.release() }
    }

    private suspend fun ensureInitialized(language: String) {
        synchronized(lock) {
            ensureNotReleased()
            if (initializedLanguage == language) return
        }
        initializeEngine(language)
        synchronized(lock) { initializedLanguage = language }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun initializeEngine(language: String) {
        try {
            engine.initialize(context, language, config)
        } catch (e: TesseractException) {
            throw mapException(e)
        } catch (e: RuntimeException) {
            throw toInitFailure(e, language)
        }
    }

    private fun ensureNotReleased() {
        if (released) throw OcrUnavailableException("Tesseract engine has been released")
    }

    private fun toRecognitionFailure(e: RuntimeException): Exception {
        if (e is CancellationException) return e
        return OcrRecognitionException("Tesseract recognition failed", e)
    }

    private fun toInitFailure(e: RuntimeException, language: String): Exception {
        if (e is CancellationException) return e
        return OcrUnavailableException("Failed to initialize Tesseract for '$language'", e)
    }

    private fun toPageResult(result: TesseractResult, language: String): OcrPageResult {
        val words = result.words.map { word ->
            OcrWord(
                text = word.text,
                confidence = word.confidence / 100f,
                boundingBox = Rect(word.boundingBox),
            )
        }
        val lines = groupIntoLines(words)
        return OcrPageResult(
            lines = lines,
            text = result.text,
            meanConfidence = words.map { it.confidence }.averageOfOrZero(),
            language = language,
        )
    }

    private fun mapException(e: TesseractException): OcrException = when (e) {
        is TesseractException.LanguageDataMissing ->
            OcrLanguageException("Language model is missing: ${e.message}", e)
        is TesseractException.NotInitialized, is TesseractException.InitFailed ->
            OcrUnavailableException("Tesseract is not initialized: ${e.message}", e)
        is TesseractException.InvalidImage ->
            OcrRecognitionException("Tesseract could not process the image", e)
        else -> OcrRecognitionException("Tesseract recognition failed: ${e.message}", e)
    }

    private fun List<Float>.averageOfOrZero(): Float =
        if (isEmpty()) 0f else sum() / size

    companion object {
        const val ID: String = "tesseract"

        private const val LINE_TOLERANCE_FRACTION = 0.6f

        /**
         * Groups words into reading-order lines by their vertical overlap. Tesseract's word
         * boxes are grouped when their vertical midpoints fall within a fraction of the
         * previous word's height.
         */
        internal fun groupIntoLines(words: List<OcrWord>): List<OcrLine> {
            if (words.isEmpty()) return emptyList()
            val sorted = words.sortedWith(
                compareBy<OcrWord> { it.boundingBox.top }.thenBy { it.boundingBox.left },
            )
            val lines = mutableListOf<MutableList<OcrWord>>()
            for (word in sorted) {
                val current = lines.lastOrNull()
                if (current != null && isSameLine(current.first(), word)) {
                    current += word
                } else {
                    lines += mutableListOf(word)
                }
            }
            return lines.map { wordsInLine ->
                val ordered = wordsInLine.sortedBy { it.boundingBox.left }
                val bounds = Rect(ordered.first().boundingBox)
                ordered.drop(1).forEach { bounds.union(it.boundingBox) }
                OcrLine(ordered, bounds)
            }
        }

        private fun isSameLine(anchor: OcrWord, candidate: OcrWord): Boolean {
            val anchorHeight = anchor.boundingBox.height().coerceAtLeast(1)
            val midpoint = (candidate.boundingBox.top + candidate.boundingBox.bottom) / 2f
            val anchorCenter = (anchor.boundingBox.top + anchor.boundingBox.bottom) / 2f
            return kotlin.math.abs(midpoint - anchorCenter) <= anchorHeight * LINE_TOLERANCE_FRACTION
        }
    }
}
