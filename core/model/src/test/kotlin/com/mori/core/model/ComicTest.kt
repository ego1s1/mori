package com.mori.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ComicTest {

    private fun comic(pageCount: Int, lastPageIndex: Int) = Comic(
        id = "c",
        title = "Title",
        series = null,
        number = null,
        format = ComicFormat.CBZ,
        pageCount = pageCount,
        sourcePath = "/lib/c.cbz",
        coverPath = null,
        lastPageIndex = lastPageIndex,
        sourceDisplayName = "c.cbz",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun untouchedComicIsNeitherInProgressNorFinished() {
        val comic = comic(pageCount = 10, lastPageIndex = 0)
        assertEquals(false, comic.isInProgress)
        assertEquals(false, comic.isFinished)
        assertEquals(0.1f, comic.progress, 0.0001f)
    }

    @Test
    fun midComicIsInProgress() {
        val comic = comic(pageCount = 10, lastPageIndex = 4)
        assertEquals(true, comic.isInProgress)
        assertEquals(false, comic.isFinished)
        assertEquals(0.5f, comic.progress, 0.0001f)
    }

    @Test
    fun lastPageIsFinished() {
        val comic = comic(pageCount = 10, lastPageIndex = 9)
        assertEquals(false, comic.isInProgress)
        assertEquals(true, comic.isFinished)
        assertEquals(1f, comic.progress, 0.0001f)
    }

    @Test
    fun emptyComicHasZeroProgressAndNoState() {
        val comic = comic(pageCount = 0, lastPageIndex = 0)
        assertEquals(false, comic.isInProgress)
        assertEquals(false, comic.isFinished)
        assertEquals(0f, comic.progress, 0.0001f)
    }

    @Test
    fun resumeIndexRestartsFinishedAndFreshBooks() {
        assertEquals(4, comic(pageCount = 10, lastPageIndex = 4).resumeIndex)
        assertEquals(0, comic(pageCount = 10, lastPageIndex = 0).resumeIndex)
        assertEquals(0, comic(pageCount = 10, lastPageIndex = 9).resumeIndex)
    }

    @Test
    fun progressClampsOvershoot() {
        val comic = comic(pageCount = 10, lastPageIndex = 50)
        assertEquals(1f, comic.progress, 0.0001f)
    }

    @Test
    fun pagesLeftCountsRemainingAfterCurrent() {
        assertEquals(7, comic(pageCount = 10, lastPageIndex = 2).pagesLeft)
        assertEquals(0, comic(pageCount = 10, lastPageIndex = 9).pagesLeft)
        assertEquals(0, comic(pageCount = 10, lastPageIndex = 50).pagesLeft)
    }

    @Test
    fun continueShelfOrdersInProgressByRecency() {
        val shelf = listOf(
            comic(pageCount = 10, lastPageIndex = 0).copy(id = "fresh"),
            comic(pageCount = 10, lastPageIndex = 3).copy(id = "old", updatedAt = 10L),
            comic(pageCount = 10, lastPageIndex = 3).copy(id = "new", updatedAt = 30L),
            comic(pageCount = 10, lastPageIndex = 9).copy(id = "done"),
        ).continueShelf()
        assertEquals(listOf("new", "old"), shelf.map { it.id })
    }

    @Test
    fun resumeTargetIsMostRecentlyTouched() {
        val list = listOf(
            comic(pageCount = 10, lastPageIndex = 1).copy(id = "a", updatedAt = 5L),
            comic(pageCount = 10, lastPageIndex = 2).copy(id = "b", updatedAt = 9L),
        )
        assertEquals("b", list.resumeTarget()?.id)
        assertEquals(null, emptyList<Comic>().resumeTarget())
    }
}
