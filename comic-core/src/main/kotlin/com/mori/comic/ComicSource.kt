package com.mori.comic

/**
 * Identifies the input a [ComicArchive] should be opened from.
 *
 * This is intentionally a sealed hierarchy so future sources (content URIs, streams,
 * remote providers) can be added without breaking the [ComicFactory] contract.
 *
 * Note: `java.io.File` is fully qualified here because the nested [File] type shadows the
 * `java.io.File` name within this class.
 */
sealed class ComicSource {
    /** A comic archive file such as `.cbz` or `.cbr`. */
    data class File(val file: java.io.File) : ComicSource()

    /** A directory of loose image files. */
    data class Directory(val directory: java.io.File) : ComicSource()
}
