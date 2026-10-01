package com.mori.core.model

/**
 * Container formats the app can import. Detection (extension + magic) and
 * the share MIME type live in comic-core's ArchiveFormat; this enum is the
 * stored/display counterpart, mapped at import time.
 */
enum class ComicFormat {
    CBZ,
    CBR,
    CB7,
    CBT,
}
