package com.mori.core.data

import com.mori.core.model.LibraryFilter
import com.mori.core.model.LibraryQuery
import com.mori.core.model.LibrarySortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryQueryFilterTest {

    private val comics = listOf(
        FakeComicsRepository.comic("b", title = "Banana", createdAt = 2L, updatedAt = 20L),
        FakeComicsRepository.comic("a", title = "apple", createdAt = 3L, updatedAt = 10L),
        FakeComicsRepository.comic(
            "c",
            title = "Cherry",
            pageCount = 5,
            lastPageIndex = 2,
            createdAt = 1L,
            updatedAt = 30L,
        ),
        FakeComicsRepository.comic(
            "d",
            title = "Date",
            pageCount = 5,
            lastPageIndex = 4,
            createdAt = 4L,
            updatedAt = 5L,
        ),
    )

    @Test
    fun titleSortIsCaseInsensitive() {
        val ids = comics.applyQuery(LibraryQuery(sortOrder = LibrarySortOrder.TITLE)).map { it.id }
        assertEquals(listOf("a", "b", "c", "d"), ids)
    }

    @Test
    fun recentlyAddedSortsByCreatedDesc() {
        val ids = comics.applyQuery(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_ADDED)).map { it.id }
        assertEquals(listOf("d", "a", "b", "c"), ids)
    }

    @Test
    fun recentlyReadSortsByUpdatedDesc() {
        val ids = comics.applyQuery(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_READ)).map { it.id }
        assertEquals(listOf("c", "b", "a", "d"), ids)
    }

    @Test
    fun unfinishedFirstPutsFinishedLast() {
        val ids = comics.applyQuery(LibraryQuery(sortOrder = LibrarySortOrder.UNFINISHED_FIRST)).map { it.id }
        assertEquals("d", ids.last())
    }

    @Test
    fun inProgressFilterMatchesStartedUnfinished() {
        val ids = comics.applyQuery(LibraryQuery(filter = LibraryFilter.IN_PROGRESS)).map { it.id }
        assertEquals(listOf("c"), ids)
    }

    @Test
    fun unreadFilterMatchesNeverStarted() {
        val ids = comics.applyQuery(LibraryQuery(filter = LibraryFilter.UNREAD)).map { it.id }
        assertEquals(setOf("a", "b"), ids.toSet())
    }

    @Test
    fun finishedFilterMatchesCompleted() {
        val ids = comics.applyQuery(LibraryQuery(filter = LibraryFilter.FINISHED)).map { it.id }
        assertEquals(listOf("d"), ids)
    }

    @Test
    fun textQueryMatchesTitleSeriesNumber() {
        val withSeries = comics + FakeComicsRepository.comic("e", title = "Egg").copy(series = "Banana Series")
        val ids = withSeries.applyQuery(LibraryQuery(text = "banana")).map { it.id }
        assertEquals(setOf("b", "e"), ids.toSet())
    }

    @Test
    fun hideErrorsDropsErroredRows() {
        val withError = comics + FakeComicsRepository.comic(
            "x",
            error = com.mori.core.model.ComicError.CORRUPT,
        )
        assertEquals(5, withError.applyQuery(LibraryQuery()).size)
        assertEquals(4, withError.applyQuery(LibraryQuery(hideErrors = true)).size)
    }
}
