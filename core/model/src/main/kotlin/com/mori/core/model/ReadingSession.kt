package com.mori.core.model

import java.util.Calendar
import java.util.TimeZone

/**
 * One reader visit: opened at [startedAt], closed at [endedAt], with
 * [pagesTurned] settled pages. Recorded once per reader close.
 */
data class ReadingSession(
    val comicId: String,
    val startedAt: Long,
    val endedAt: Long,
    val pagesTurned: Int,
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0L)
}

/**
 * Reading totals bucketed into one local day, newest first when produced by
 * [dailyReadingStats].
 */
data class DailyReadingStat(
    val dayStartMillis: Long,
    val durationMs: Long,
    val pagesTurned: Int,
    val sessions: Int,
)

/**
 * Buckets sessions into the last [days] local days ending today, newest
 * first. Days without reading are still emitted with zeroes so a chart keeps
 * a steady axis. Pure for testability.
 */
fun List<ReadingSession>.dailyReadingStats(
    nowMillis: Long,
    days: Int,
): List<DailyReadingStat> {
    if (days <= 0) return emptyList()
    val today = dayStartMillis(nowMillis)
    val buckets = LinkedHashMap<Long, DailyReadingStat>()
    var day = today
    repeat(days) {
        buckets[day] = DailyReadingStat(day, 0L, 0, 0)
        day = previousDayStartMillis(day)
    }
    forEach { session ->
        val key = dayStartMillis(session.startedAt)
        val bucket = buckets[key]
        if (bucket != null) {
            buckets[key] = bucket.copy(
                durationMs = bucket.durationMs + session.durationMs,
                pagesTurned = bucket.pagesTurned + session.pagesTurned,
                sessions = bucket.sessions + 1,
            )
        }
    }
    return buckets.values.toList()
}

/** Local-midnight millis containing [millis] in [zone]. */
fun dayStartMillis(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

/** Local-midnight millis of the day before [dayStart] (DST-safe). */
fun previousDayStartMillis(dayStart: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = dayStart
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
