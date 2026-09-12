package com.mori.feature.reader.impl

import com.mori.core.model.PageHalf
import com.mori.core.model.ReadingDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderDualPageTest {

    @Test
    fun noWidePagesIsIdentity() {
        val pages = buildViewerPages(3, emptySet(), ReadingDirection.LEFT_TO_RIGHT, invert = false)

        assertEquals(
            listOf(
                ReaderViewerPage(0, PageHalf.FULL),
                ReaderViewerPage(1, PageHalf.FULL),
                ReaderViewerPage(2, PageHalf.FULL),
            ),
            pages,
        )
        assertEquals(listOf(0, 1, 2), archiveToExpandedPositions(pages, 3))
    }

    @Test
    fun ltrWidePageReadsLeftThenRight() {
        val pages = buildViewerPages(3, setOf(1), ReadingDirection.LEFT_TO_RIGHT, invert = false)

        assertEquals(4, pages.size)
        assertEquals(ReaderViewerPage(0, PageHalf.FULL), pages[0])
        assertEquals(ReaderViewerPage(1, PageHalf.LEFT), pages[1])
        assertEquals(ReaderViewerPage(1, PageHalf.RIGHT), pages[2])
        assertEquals(ReaderViewerPage(2, PageHalf.FULL), pages[3])
        assertEquals(listOf(0, 1, 3), archiveToExpandedPositions(pages, 3))
    }

    @Test
    fun rtlWidePageReadsRightThenLeft() {
        val pages = buildViewerPages(2, setOf(0), ReadingDirection.RIGHT_TO_LEFT, invert = false)

        assertEquals(ReaderViewerPage(0, PageHalf.RIGHT), pages[0])
        assertEquals(ReaderViewerPage(0, PageHalf.LEFT), pages[1])
        assertEquals(ReaderViewerPage(1, PageHalf.FULL), pages[2])
    }

    @Test
    fun invertSwapsHalves() {
        val pages = buildViewerPages(1, setOf(0), ReadingDirection.LEFT_TO_RIGHT, invert = true)

        assertEquals(
            listOf(
                ReaderViewerPage(0, PageHalf.RIGHT),
                ReaderViewerPage(0, PageHalf.LEFT),
            ),
            pages,
        )
    }

    @Test
    fun outOfRangeWideEntriesAreIgnored() {
        val pages = buildViewerPages(2, setOf(-1, 2, 99), ReadingDirection.LEFT_TO_RIGHT, invert = false)

        assertEquals(
            listOf(
                ReaderViewerPage(0, PageHalf.FULL),
                ReaderViewerPage(1, PageHalf.FULL),
            ),
            pages,
        )
    }

    @Test
    fun emptyBookStaysEmpty() {
        assertEquals(emptyList<ReaderViewerPage>(), buildViewerPages(0, setOf(0), ReadingDirection.LEFT_TO_RIGHT, false))
        assertEquals(emptyList<Int>(), archiveToExpandedPositions(emptyList(), 0))
    }
}
