package com.mori.core.data

import com.mori.core.model.Comic
import com.mori.core.model.DisplayFilter
import com.mori.core.model.ImportReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ReadingStats
import com.mori.core.model.StorageUsage
import kotlinx.coroutines.flow.Flow

/**
 * Single public API for library data. Room is the source of truth; the
 * library reads user folders in place (never copies), covers live in
 * app-private storage.
 */
interface ComicsRepository {
    /** Reactive library rows with [LibraryQuery] applied in memory. */
    fun observeLibrary(query: LibraryQuery = LibraryQuery()): Flow<List<Comic>>

    fun observeComic(id: String): Flow<Comic?>

    /** One-shot read for non-reactive consumers (image loading, widgets). */
    suspend fun getComic(id: String): Comic?

    /**
     * Indexes a user-linked folder in place: no files are copied, archives are
     * read through transient cache materializations, and rows address documents
     * by URI so rescans stay fresh without duplicating the collection.
     */
    suspend fun indexLinkedTree(
        treeUri: android.net.Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportReport

    /** Re-indexes a single comic (e.g. after retrying a failed one). */
    suspend fun refreshComic(id: String): Comic?

    /** Unlinks the index row (and its cover). User originals are never touched. */
    suspend fun removeComic(id: String)

    suspend fun saveProgress(id: String, pageIndex: Int)

    suspend fun toggleBookmark(id: String)

    /**
     * Per-comic display-filter override: suspending read plus a reactive
     * stream for the reader. Null means "no override, use the global
     * default from reader preferences".
     */
    fun observeDisplayFilter(id: String): Flow<DisplayFilter?>

    suspend fun getDisplayFilter(id: String): DisplayFilter?

    /** Upserts the override; a neutral filter deletes the row (reset). */
    suspend fun setDisplayFilter(id: String, filter: DisplayFilter)

    /** Deletes the override, restoring the global default. */
    suspend fun clearDisplayFilter(id: String)

    /**
     * Records one reader visit. Called once per reader close with the
     * session's wall time and settled turns; a no-op for empty visits is
     * fine, callers decide.
     */
    suspend fun recordSession(comicId: String, startedAt: Long, endedAt: Long, pagesTurned: Int)

    /** Reactive reading aggregates for stats surfaces. */
    fun observeReadingStats(): Flow<ReadingStats>

    /**
     * Archive indices of wide (landscape) pages, for the dual-page split.
     *
     * Bounds-decodes every page, so callers only invoke it when the split is
     * enabled — and treat an empty set as "no info" (scan failures also yield
     * empty, falling back to whole pages). Results are deterministic per file,
     * so callers memoize per comic.
     */
    suspend fun widePageIndices(id: String): Set<Int>

    /**
     * Deletes generated covers and their index references (regenerated on
     * rescan) plus the transient link-read cache.
     */
    suspend fun clearThumbnailCache()

    /** Measures the app-private footprint (covers, read cache) for storage settings. */
    suspend fun storageUsage(): StorageUsage
}
