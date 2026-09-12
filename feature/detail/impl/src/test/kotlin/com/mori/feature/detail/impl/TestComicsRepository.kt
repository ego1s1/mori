package com.mori.feature.detail.impl

import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import com.mori.core.model.LibraryQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository double for detail tests. */
internal class TestComicsRepository(
    initial: Map<String, Comic> = emptyMap(),
) : ComicsRepository {

    private val comics = MutableStateFlow(initial)
    var refreshCalls = 0
    var failRefreshWith: Exception? = null
    val removedIds = mutableListOf<String>()
    val progressSaves = mutableListOf<Pair<String, Int>>()

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        comics.asStateFlow().map { it.values.toList() }

    override fun observeComic(id: String): Flow<Comic?> =
        comics.asStateFlow().map { it[id] }

    override suspend fun getComic(id: String): Comic? = comics.value[id]

    override suspend fun indexLinkedTree(
        treeUri: android.net.Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): com.mori.core.model.ImportReport =
        com.mori.core.model.ImportReport(0, 0, 0, emptyList())

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
    }

    override suspend fun widePageIndices(id: String): Set<Int> = emptySet()

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.mapValues { (_, comic) ->
            if (comic.id == id) comic.copy(bookmarked = !comic.bookmarked) else comic
        }
    }

    override suspend fun clearThumbnailCache() {
    }

    override suspend fun storageUsage(): com.mori.core.model.StorageUsage =
        com.mori.core.model.StorageUsage(0, 0L, 0L)

    companion object {
        fun comic(
            id: String,
            title: String = "Title $id",
            pageCount: Int = 10,
            lastPageIndex: Int = 0,
            error: ComicError? = null,
        ) = Comic(
            id = id,
            title = title,
            series = null,
            number = null,
            format = ComicFormat.CBZ,
            pageCount = pageCount,
            sourcePath = "/lib/$id.cbz",
            coverPath = null,
            lastPageIndex = lastPageIndex,
            sourceDisplayName = "$id.cbz",
            createdAt = 1L,
            updatedAt = 1L,
            error = error,
        )
    }
}
