package com.mori.core.model

/** Container formats the app can import. */
enum class ComicFormat {
    CBZ,
    CBR,
    CB7,
    CBT,
    ;

    companion object {
        fun fromFileName(name: String): ComicFormat? {
            val extension = name.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "cbz", "zip" -> CBZ
                "cbr", "rar" -> CBR
                "cb7", "7z" -> CB7
                "cbt", "tar" -> CBT
                else -> null
            }
        }
    }

    /** MIME type for sharing the container file. */
    fun mimeType(): String = when (this) {
        CBZ -> "application/zip"
        CBR -> "application/vnd.rar"
        CB7 -> "application/x-7z-compressed"
        CBT -> "application/x-tar"
    }
}
