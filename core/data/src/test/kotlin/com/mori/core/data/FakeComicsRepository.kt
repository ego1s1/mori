package com.mori.core.data

import com.mori.core.model.Comic
import com.mori.core.model.ComicError
import com.mori.core.model.ComicFormat
import com.mori.core.model.DisplayFilter
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

    override suspend fun widePageIndices(id: String): Set<Int> = emptySet()

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.map {
            if (it.id == id) it.copy(bookmarked = !it.bookmarked) else it
        }
    }

    private val displayFilters = mutableMapOf<String, DisplayFilter?>()
    private val filterVersions = MutableStateFlow(0)

    override fun observeDisplayFilter(id: String): Flow<DisplayFilter?> =
        filterVersions.map { displayFilters[id] }

    override suspend fun getDisplayFilter(id: String): DisplayFilter? = displayFilters[id]

    override suspend fun setDisplayFilter(id: String, filter: DisplayFilter) {
        displayFilters[id] = filter.coerce().takeUnless { it.isNeutral }
        filterVersions.value += 1
    }

    override suspend fun clearDisplayFilter(id: String) {
        displayFilters.remove(id)
        filterVersions.value += 1
    }

    private var nextCollectionId = 1L
    private val collectionsState =
        MutableStateFlow(emptyList<com.mori.core.model.UserCollection>())
    private val memberships = mutableMapOf<Long, MutableSet<String>>()

    override fun observeCollections(): Flow<List<com.mori.core.model.UserCollection>> =
        collectionsState.asStateFlow()

    override fun observeCollectionMembers(collectionId: Long): Flow<Set<String>> =
        collectionsState.map { memberships[collectionId].orEmpty().toSet() }

    override fun observeComicCollections(comicId: String): Flow<Set<Long>> =
        collectionsState.map { _ ->
            memberships.filterValues { comicId in it }.keys.toSet()
        }

    override suspend fun createCollection(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val id = nextCollectionId++
        collectionsState.value += com.mori.core.model.UserCollection(
            id = id,
            name = trimmed,
            bookCount = 0,
            createdAt = id,
        )
        return id
    }

    override suspend fun renameCollection(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        collectionsState.value = collectionsState.value.map {
            if (it.id == id) it.copy(name = trimmed) else it
        }
    }

    override suspend fun deleteCollection(id: Long) {
        collectionsState.value = collectionsState.value.filterNot { it.id == id }
        memberships.remove(id)
    }

    override suspend fun addToCollection(collectionId: Long, comicId: String) {
        memberships.getOrPut(collectionId) { mutableSetOf() } += comicId
        collectionsState.value = collectionsState.value.map {
            if (it.id == collectionId) {
                it.copy(bookCount = memberships[it.id].orEmpty().size)
            } else {
                it
            }
        }
    }

    override suspend fun removeFromCollection(collectionId: Long, comicId: String) {
        memberships[collectionId]?.remove(comicId)
        collectionsState.value = collectionsState.value.map {
            if (it.id == collectionId) {
                it.copy(bookCount = memberships[it.id].orEmpty().size)
            } else {
                it
            }
        }
    }

    val sessions = mutableListOf<Triple<String, Long, Int>>()

    override suspend fun recordSession(
        comicId: String,
        startedAt: Long,
        endedAt: Long,
        pagesTurned: Int,
    ) {
        sessions += Triple(comicId, maxOf(endedAt - startedAt, 0L), pagesTurned)
    }

    override fun observeReadingStats(): Flow<com.mori.core.model.ReadingStats> =
        kotlinx.coroutines.flow.flowOf(
            com.mori.core.model.ReadingStats(
                totalSessions = sessions.size,
                totalDurationMs = sessions.sumOf { it.second },
                totalPagesTurned = sessions.sumOf { it.third },
                booksFinished = 0,
            ),
        )

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
