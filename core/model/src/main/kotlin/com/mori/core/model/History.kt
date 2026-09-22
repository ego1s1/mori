package com.mori.core.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Reading history: Mori tracks whole comics,
 * so one entry per touched book, ordered by recency and bucketed by local
 * day (Today / Yesterday / date headers in the UI).
 *
 * Pure Kotlin with no Android dependencies; day math runs on
 * [java.util.Calendar], available on every API level without desugaring.
 */
data class HistoryDay(
    /** Local-midnight millis starting this bucket. */
    val dayStartMillis: Long,
    /** Touched comics read that day, most recent first. */
    val comics: List<Comic>,
)

/**
 * Touched books (anything opened past the cover) grouped by local day.
 * Untouched books have nothing to resume; errored rows stay visible but the
 * UI routes their taps to details, like the library cards. Future-dated
 * rows (clock skew, restored backups) clamp to today instead of forming a
 * bucket in the future; same-millis ties break by id so ordering never
 * depends on repository emission order.
 */
fun List<Comic>.historyGroups(
    nowMillis: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<HistoryDay> {
    val touched = filter { it.lastPageIndex > 0 }
        .sortedWith(compareByDescending<Comic> { minOf(it.updatedAt, nowMillis) }.thenBy { it.id })
    if (touched.isEmpty()) return emptyList()
    return touched.groupBy { dayStartMillis(minOf(it.updatedAt, nowMillis), zone) }
        .map { (dayStart, comics) -> HistoryDay(dayStart, comics) }
        .sortedByDescending { it.dayStartMillis }
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
