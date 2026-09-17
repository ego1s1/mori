package com.mori.core.model

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun comic(id: String, lastPageIndex: Int, updatedAt: Long) = Comic(
        id = id,
        title = "Title $id",
        series = null,
        number = null,
        format = ComicFormat.CBZ,
        pageCount = 10,
        sourcePath = "/lib/$id.cbz",
        coverPath = null,
        lastPageIndex = lastPageIndex,
        sourceDisplayName = "$id.cbz",
        createdAt = 1L,
        updatedAt = updatedAt,
    )

    @Test
    fun untouchedBooksHaveNoHistory() {
        val comics = listOf(comic("a", lastPageIndex = 0, updatedAt = 1_000L))
        assertTrue(comics.historyGroups(nowMillis = 2_000L, zone = utc).isEmpty())
        assertTrue(emptyList<Comic>().historyGroups(nowMillis = 2_000L, zone = utc).isEmpty())
    }

    @Test
    fun groupsByLocalDayMostRecentFirst() {
        // 2026-09-16 and 2026-09-17 UTC midnights.
        val day1 = 1_789_516_800_000L
        val day2 = day1 + 86_400_000L
        val comics = listOf(
            comic("old", lastPageIndex = 2, updatedAt = day1 + 3_600_000L),
            comic("new-morning", lastPageIndex = 1, updatedAt = day2 + 3_600_000L),
            comic("new-evening", lastPageIndex = 4, updatedAt = day2 + 36_000_000L),
            comic("fresh", lastPageIndex = 0, updatedAt = day2 + 40_000_000L),
        )
        val groups = comics.historyGroups(nowMillis = day2 + 40_000_000L, zone = utc)

        assertEquals(2, groups.size)
        assertEquals(day2, groups[0].dayStartMillis)
        assertEquals(listOf("new-evening", "new-morning"), groups[0].comics.map { it.id })
        assertEquals(day1, groups[1].dayStartMillis)
        assertEquals(listOf("old"), groups[1].comics.map { it.id })
    }

    @Test
    fun dayStartTruncatesToMidnight() {
        assertEquals(1_789_603_200_000L, dayStartMillis(1_789_603_200_000L + 45_000_123L, utc))
    }
}
