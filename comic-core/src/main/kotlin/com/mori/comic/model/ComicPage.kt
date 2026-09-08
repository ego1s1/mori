package com.mori.comic.model

/**
 * A single readable page inside a [com.mori.comic.ComicArchive].
 *
 * @property index zero-based position after natural ordering.
 * @property name the archive entry name or file name that identifies the page.
 * @property mediaType the image container format of the page.
 * @property sizeBytes the uncompressed size of the page in bytes, or `-1` when unknown.
 */
data class ComicPage(
    val index: Int,
    val name: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
)
