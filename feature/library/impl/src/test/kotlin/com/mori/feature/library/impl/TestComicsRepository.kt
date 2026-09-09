package com.mori.feature.library.impl

import com.mori.core.data.ComicsRepository
import com.mori.core.data.applyQuery
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository double applying real query semantics. */
internal class TestComicsRepository(
    initial: List<Comic> = emptyList(),
) : ComicsRepository {

    private val comics = MutableStateFlow(initial)
    var refreshReport = IndexReport(0, 0, 0)
    var refreshCalls = 0
    var failRefreshWith: Exception? = null
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

    override suspend fun refreshLibrary(): IndexReport {
        refreshCalls += 1
        failRefreshWith?.let { throw it }
        return refreshReport
    }

    override suspend fun refreshComic(id: String): Comic? = getComic(id)

    override suspend fun removeComic(id: String) {
        comics.value = comics.value.filterNot { it.id == id }
    }

    override suspend fun saveProgress(id: String, pageIndex: Int) {
        progressSaves += id to pageIndex
    }

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.map {
            if (it.id == id) it.copy(bookmarked = !it.bookmarked) else it
        }
    }    companion object {
        fun comic(
            id: String,
            title: String = "Title $id",
            pageCount: Int = 10,
            lastPageIndex: Int = 0,
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
        )
    }
}
