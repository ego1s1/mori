package com.mori.comic

import com.mori.comic.model.ComicMetadata
import com.mori.comic.model.ComicPage
import java.io.Closeable

/**
 * A decoded, read-only view of a comic archive or folder of images.
 *
 * Implementations are opened via [ComicFactory] and must be [close]d when no longer
 * needed to release underlying file handles. Once closed, any further call throws
 * [ArchiveClosedException].
 */
interface ComicArchive : Closeable {
    /** The source this archive was opened from. */
    val source: ComicSource

    /** Parsed `ComicInfo.xml` metadata, or an empty instance when absent. */
    val metadata: ComicMetadata

    /** All readable pages, naturally ordered by entry/file name. */
    val pages: List<ComicPage>

    /** Convenience accessor for [pages].size. */
    val pageCount: Int
        get() = pages.size

    /**
     * Reads the full, uncompressed bytes of [page].
     *
     * The read is performed off the caller's thread on [kotlinx.coroutines.Dispatchers.IO].
     * The returned array should be treated as transient: callers decoding large pages
     * should prefer the decode APIs introduced in later phases.
     *
     * @throws PageNotFoundException when [page] does not belong to this archive.
     * @throws ArchiveClosedException when the archive has been closed.
     * @throws CorruptArchiveException when the underlying data cannot be decompressed.
     */
    suspend fun readPage(page: ComicPage): ByteArray
}
