package com.mori.core.model

/**
 * Container formats the app can import. Detection (extension + magic) lives
 * in comic-core's ArchiveFormat; this enum is the stored/display
 * counterpart, mapped at import time.
 */
enum class ComicFormat {
    CBZ,
    CBR,
    CB7,
    CBT,
    ;

    /** MIME type for sharing the container file. */
    fun mimeType(): String = when (this) {
        CBZ -> "application/zip"
        CBR -> "application/vnd.rar"
        CB7 -> "application/x-7z-compressed"
        CBT -> "application/x-tar"
    }
}
