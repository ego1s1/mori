package com.mori.core.data

import com.mori.core.model.Comic
import com.mori.core.model.ImportReport
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.StorageUsage
import kotlinx.coroutines.flow.Flow

/**
 * Single public API for library data. Room is the source of truth; file bytes and covers
 * live in app-private storage.
 */
interface ComicsRepository {
    /** Reactive library rows with [LibraryQuery] applied in memory. */
    fun observeLibrary(query: LibraryQuery = LibraryQuery()): Flow<List<Comic>>

    fun observeComic(id: String): Flow<Comic?>

    /** One-shot read for non-reactive consumers (image loading, widgets). */
    suspend fun getComic(id: String): Comic?

    /**
     * Scans the library directory, indexing new/changed archives and dropping rows whose
     * files are gone. Covers regenerate only when missing or stale. Linked rows
     * (custom folders) are never touched here; see [indexLinkedTree].
     */
    suspend fun refreshLibrary(): IndexReport

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

    /** Deletes the source file, cover, and index row. Missing pieces are tolerated. */
    suspend fun removeComic(id: String)

    suspend fun saveProgress(id: String, pageIndex: Int)

    suspend fun toggleBookmark(id: String)

    /** Deletes generated covers and clears their index references (regenerated on rescan). */
    suspend fun clearThumbnailCache()

    /** Measures the app-private library footprint for the storage manager. */
    suspend fun storageUsage(): StorageUsage
}
