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
    fun progressClampsOvershoot() {
        val comic = comic(pageCount = 10, lastPageIndex = 50)
        assertEquals(1f, comic.progress, 0.0001f)
    }
}
