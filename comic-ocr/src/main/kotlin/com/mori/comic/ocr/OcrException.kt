package com.mori.comic.ocr

/** Base type for OCR failures. */
sealed class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The engine has not been initialized, or its native backend is unavailable. */
class OcrUnavailableException(message: String, cause: Throwable? = null) : OcrException(message, cause)

/** Recognition failed mid-operation (e.g. a page the engine cannot process). */
class OcrRecognitionException(message: String, cause: Throwable? = null) : OcrException(message, cause)

/** The requested language model is not available. */
class OcrLanguageException(message: String, cause: Throwable? = null) : OcrException(message, cause)
