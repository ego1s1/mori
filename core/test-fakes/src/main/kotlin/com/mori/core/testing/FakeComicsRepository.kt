package com.mori.core.testing

import android.net.Uri
import com.mori.core.data.ComicsRepository
import com.mori.core.data.applyQuery
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import com.mori.core.model.ImportReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.StorageUsage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Single in-memory [ComicsRepository] double for all suites. It applies the
 * real [applyQuery] semantics and records every write, so tests configure
 * hooks ([widePages], [failLinkWith], [failRefreshWith], [linkReport])
 * instead of forking the double per feature.
 */
class FakeComicsRepository(
    initial: Map<String, Comic> = emptyMap(),
) : ComicsRepository {

    constructor(initial: List<Comic>) : this(initial.associateBy { it.id })

    private val comics = MutableStateFlow(initial)
    var linkReport = ImportReport(0, 0, 0, emptyList())
    val linkedTrees = mutableListOf<Uri>()
    var failLinkWith: Exception? = null
    var failRefreshWith: Exception? = null
    var refreshCalls = 0
    val removedIds = mutableListOf<String>()
    val progressSaves = mutableListOf<Pair<String, Int>>()
    var widePages: Set<Int> = emptySet()
    /** Optional gate held inside indexLinkedTree so tests can overlap runs. */
    var indexGate: CompletableDeferred<Unit>? = null
    var usage = StorageUsage(0, 0L, 0L)
    var clearCacheCalls = 0

    fun send(comics: List<Comic>) {
        this.comics.value = comics.associateBy { it.id }
    }

    fun send(comic: Comic) {
        comics.value = comics.value + (comic.id to comic)
    }

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        comics.asStateFlow().map { it.values.toList().applyQuery(query) }

    override fun observeComic(id: String): Flow<Comic?> =
        comics.asStateFlow().map { it[id] }

    override suspend fun getComic(id: String): Comic? = comics.value[id]

    override suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ImportReport {
        linkedTrees += treeUri
        indexGate?.await()
        failLinkWith?.let { throw it }
        return linkReport
    }

    override suspend fun refreshComic(id: String): Comic? {
        refreshCalls += 1
        failRefreshWith?.let { throw it }
        return getComic(id)
    }

    override suspend fun removeComic(id: String) {
        removedIds += id
        failRefreshWith?.let { throw it }
        comics.value = comics.value - id
    }

    override suspend fun saveProgress(id: String, pageIndex: Int) {
        progressSaves += id to pageIndex
        comics.value = comics.value.mapValues { (_, comic) ->
            if (comic.id == id) comic.copy(lastPageIndex = pageIndex) else comic
        }
    }

    override suspend fun widePageIndices(id: String): Set<Int> = widePages

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.mapValues { (_, comic) ->
            if (comic.id == id) comic.copy(bookmarked = !comic.bookmarked) else comic
        }
    }

    override suspend fun clearThumbnailCache() {
        clearCacheCalls += 1
        usage = usage.copy(coversBytes = 0L)
    }

    override suspend fun storageUsage(): StorageUsage = usage

    companion object {
        fun comic(
            id: String,
            title: String = "Title $id",
            pageCount: Int = 10,
            lastPageIndex: Int = 0,
            error: ComicError? = null,
            bookmarked: Boolean = false,
            series: String? = null,
            number: String? = null,
            createdAt: Long = 1L,
            updatedAt: Long = 1L,
        ) = Comic(
            id = id,
            title = title,
            series = series,
            number = number,
            format = ComicFormat.CBZ,
            pageCount = pageCount,
            sourcePath = "/lib/$id.cbz",
            coverPath = null,
            lastPageIndex = lastPageIndex,
            sourceDisplayName = "$id.cbz",
            createdAt = createdAt,
            updatedAt = updatedAt,
            error = error,
            bookmarked = bookmarked,
        )
    }
}
