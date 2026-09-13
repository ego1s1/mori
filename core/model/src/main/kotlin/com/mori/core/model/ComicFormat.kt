package com.mori.core.model

/** Container formats the app can import. */
enum class ComicFormat {
    CBZ,
    CBR,
    ;

    companion object {
        fun fromFileName(name: String): ComicFormat? {
            val extension = name.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "cbz", "zip" -> CBZ
                "cbr", "rar" -> CBR
                else -> null
            }
        }
    }

    /** MIME type for sharing the container file. */
    fun mimeType(): String = when (this) {
        CBZ -> "application/zip"
        CBR -> "application/vnd.rar"
    }
}
