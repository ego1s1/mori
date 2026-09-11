package com.mori.core.data

import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository double with hooks for scripted comics. */
internal class FakeComicsRepository(
    initial: List<Comic> = emptyList(),
) : ComicsRepository {

    private val comics = MutableStateFlow(initial)
    var refreshLibraryReport = IndexReport(0, 0, 0)
    val removedIds = mutableListOf<String>()
    val linkedTrees = mutableListOf<android.net.Uri>()
    val progressSaves = mutableListOf<Pair<String, Int>>()

    fun send(comics: List<Comic>) {
        this.comics.value = comics
    }

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        comics.asStateFlow().map { it.applyQuery(query) }

    override fun observeComic(id: String): Flow<Comic?> =
        comics.asStateFlow().map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getComic(id: String): Comic? =
        comics.value.firstOrNull { it.id == id }

    override suspend fun refreshLibrary(): IndexReport = refreshLibraryReport

    override suspend fun indexLinkedTree(
        treeUri: android.net.Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): com.mori.core.model.ImportReport {
        linkedTrees += treeUri
        return com.mori.core.model.ImportReport(0, 0, 0, emptyList())
    }

    override suspend fun refreshComic(id: String): Comic? = getComic(id)

    override suspend fun removeComic(id: String) {
        removedIds += id
        comics.value = comics.value.filterNot { it.id == id }
    }

    override suspend fun saveProgress(id: String, pageIndex: Int) {
        progressSaves += id to pageIndex
        comics.value = comics.value.map {
            if (it.id == id) it.copy(lastPageIndex = pageIndex) else it
        }
    }

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.map {
            if (it.id == id) it.copy(bookmarked = !it.bookmarked) else it
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
            createdAt: Long = 1L,
            updatedAt: Long = 1L,
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
            createdAt = createdAt,
            updatedAt = updatedAt,
            error = error,
        )
    }
}
