package com.mori.feature.reader.impl

import com.mori.core.data.ComicsRepository
import com.mori.core.datastore.MoriPreferencesDataSource
import com.mori.core.model.Comic
import com.mori.core.model.ComicFormat
import com.mori.core.model.IndexReport
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ReaderPreferences
import com.mori.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository double for reader tests. */
internal class TestComicsRepository(
    initial: Map<String, Comic> = emptyMap(),
) : ComicsRepository {

    private val comics = MutableStateFlow(initial)
    val progressSaves = mutableListOf<Pair<String, Int>>()

    fun send(comic: Comic) {
        comics.value = comics.value + (comic.id to comic)
    }

    override fun observeLibrary(query: LibraryQuery): Flow<List<Comic>> =
        comics.asStateFlow().map { it.values.toList() }

    override fun observeComic(id: String): Flow<Comic?> =
        comics.asStateFlow().map { it[id] }

    override suspend fun getComic(id: String): Comic? = comics.value[id]

    override suspend fun refreshLibrary(): IndexReport = IndexReport(0, 0, 0)

    override suspend fun refreshComic(id: String): Comic? = getComic(id)

    override suspend fun removeComic(id: String) {
        comics.value = comics.value - id
    }

    override suspend fun saveProgress(id: String, pageIndex: Int) {
        progressSaves += id to pageIndex
        comics.value = comics.value.mapValues { (_, comic) ->
            if (comic.id == id) comic.copy(lastPageIndex = pageIndex) else comic
        }
    }

    override suspend fun toggleBookmark(id: String) {
        comics.value = comics.value.mapValues { (_, comic) ->
            if (comic.id == id) comic.copy(bookmarked = !comic.bookmarked) else comic
        }
    }

    companion object {
        fun comic(
            id: String,
            title: String = "Title $id",
            pageCount: Int = 10,
            lastPageIndex: Int = 0,
            bookmarked: Boolean = false,
            error: com.mori.core.model.ComicError? = null,
        ) = Comic(
            id = id,
            title = title,
            series = "Series",
            number = "1",
            format = ComicFormat.CBZ,
            pageCount = pageCount,
            sourcePath = "/lib/$id.cbz",
            coverPath = null,
            lastPageIndex = lastPageIndex,
            sourceDisplayName = "$id.cbz",
            createdAt = 1L,
            updatedAt = 1L,
            error = error,
            bookmarked = bookmarked,
        )
    }
}

/** In-memory preferences double. */
internal class TestPreferencesDataSource(
    initial: ReaderPreferences = ReaderPreferences(),
) : MoriPreferencesDataSource {

    private val completed = MutableStateFlow(false)
    private val treeUri = MutableStateFlow<String?>(null)
    private val readerPreferencesFlow = MutableStateFlow(initial)
    private val themePreferencesFlow = MutableStateFlow(ThemePreferences())
    val updates = mutableListOf<ReaderPreferences>()

    override val onboardingCompleted: Flow<Boolean> = completed.asStateFlow()
    override val sourceTreeUri: Flow<String?> = treeUri.asStateFlow()
    override val readerPreferences: Flow<ReaderPreferences> = readerPreferencesFlow.asStateFlow()
    override val themePreferences: Flow<ThemePreferences> = themePreferencesFlow.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        this.completed.value = completed
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        treeUri.value = uri
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(readerPreferencesFlow.value)
        updates += updated
        readerPreferencesFlow.value = updated
    }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        themePreferencesFlow.value = transform(themePreferencesFlow.value)
    }
}
