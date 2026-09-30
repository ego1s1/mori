package com.mori.core.model

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingSessionTest {

    private fun withUtcZone(block: () -> Unit) {
        val previous = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            block()
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun sessionsBucketByDayNotExactTimestamp() = withUtcZone {
        // Regression: buckets were keyed on the exact session timestamp, so a
        // mid-day session never matched its day-start key and charts read zero.
        val day = dayStartMillis(1_700_000_000_000L)
        val sessions = listOf(
            ReadingSession("a", day + 3_600_000L, day + 5_400_000L, pagesTurned = 10),
            ReadingSession("a", day + 7_200_000L, day + 7_800_000L, pagesTurned = 5),
        )
        val stats = sessions.dailyReadingStats(nowMillis = day + 86_000_000L, days = 2)

        assertEquals(2, stats.size)
        assertEquals(day, stats[0].dayStartMillis)
        assertEquals(2_400_000L, stats[0].durationMs)
        assertEquals(15, stats[0].pagesTurned)
        assertEquals(2, stats[0].sessions)
        assertEquals(0L, stats[1].durationMs)
    }

    @Test
    fun bucketsSpanDaysNewestFirst() = withUtcZone {
        val today = dayStartMillis(1_700_000_000_000L)
        val yesterday = previousDayStartMillis(today)
        val sessions = listOf(
            ReadingSession("a", yesterday + 1_000L, yesterday + 61_000L, pagesTurned = 3),
        )
        val stats = sessions.dailyReadingStats(nowMillis = today + 1_000L, days = 2)

        assertEquals(listOf(today, yesterday), stats.map { it.dayStartMillis })
        assertEquals(0L, stats[0].durationMs)
        assertEquals(60_000L, stats[1].durationMs)
        assertEquals(3, stats[1].pagesTurned)
    }

    @Test
    fun emptyDaysStillEmitted() = withUtcZone {
        val today = dayStartMillis(1_700_000_000_000L)
        val stats = emptyList<ReadingSession>().dailyReadingStats(nowMillis = today, days = 3)

        assertEquals(3, stats.size)
        assertEquals(true, stats.all { it.durationMs == 0L && it.sessions == 0 })
    }
}
