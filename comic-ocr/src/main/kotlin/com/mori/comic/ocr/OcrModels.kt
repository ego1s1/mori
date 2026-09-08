package com.mori.comic.ocr

import android.graphics.Rect

/**
 * A recognized word with its text, confidence, and bounding box (in page pixel coordinates).
 */
data class OcrWord(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect,
)

/**
 * A recognized line of text composed of one or more [OcrWord]s.
 */
data class OcrLine(
    val words: List<OcrWord>,
    val boundingBox: Rect,
) {
    /** The concatenated text of the line, words joined by a single space. */
    val text: String get() = words.joinToString(" ") { it.text }
}

/**
 * The complete result of recognizing a page.
 *
 * @property lines recognized lines, in reading order.
 * @property text plain-text transcription of the whole page.
 * @property meanConfidence average word confidence in `0f..1f`, or `0f` with no words.
 * @property language the language code actually used.
 */
data class OcrPageResult(
    val lines: List<OcrLine>,
    val text: String,
    val meanConfidence: Float,
    val language: String,
) {
    val isEmpty: Boolean get() = lines.isEmpty() || text.isBlank()
}

/**
 * A single recognized symbol for the low-level character path; consumed by engines that
 * expose per-character data. Most consumers use [OcrPageResult].
 */
data class OcrSymbol(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect,
)
