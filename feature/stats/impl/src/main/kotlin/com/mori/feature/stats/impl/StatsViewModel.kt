package com.mori.feature.stats.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mori.core.data.ComicsRepository
import com.mori.core.model.Comic
import com.mori.core.model.DailyReadingStat
import com.mori.core.model.LibraryQuery
import com.mori.core.model.ReadingSession
import com.mori.core.model.dayStartMillis
import com.mori.core.model.dailyReadingStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Reading stats surface. Aggregates the raw session log into the windows the
 * screen shows (chart buckets, streak, top books); all shaping is pure and
 * unit-tested so the composable stays declarative.
 */
@HiltViewModel
internal class StatsViewModel @Inject constructor(
    repository: ComicsRepository,
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.WEEK)

    val uiState: StateFlow<StatsUiState> = combine(
        repository.observeReadingStats(),
        repository.observeReadingSessions(),
        repository.observeLibrary(LibraryQuery()),
        range,
    ) { totals, sessions, comics, selectedRange ->
        StatsUiState.Success(
            totals = totals,
            buckets = sessions.dailyReadingStats(
                nowMillis = System.currentTimeMillis(),
                days = selectedRange.days(),
            ).reversed(),
            range = selectedRange,
            streak = readingStreak(sessions, System.currentTimeMillis()),
            topBooks = topBooks(sessions, comics),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState.Loading,
    )

    fun onAction(action: StatsAction) {
        when (action) {
            is StatsAction.SelectRange -> range.update { action.range }
        }
    }
}

/** Window length in days for a chart range. */
internal fun StatsRange.days(): Int = when (this) {
    StatsRange.WEEK -> 7
    StatsRange.MONTH -> 30
    StatsRange.YEAR -> 365
}

/**
 * Current and longest consecutive-day streaks from the set of days that have
 * at least one session. The current streak counts back from today (or
 * yesterday, so a streak isn't shown broken until a full day is missed).
 * Pure for testability.
 */
internal fun readingStreak(sessions: List<ReadingSession>, nowMillis: Long): StreakInfo {
    if (sessions.isEmpty()) return StreakInfo(current = 0, longest = 0)
    val days = sessions.map { dayStartMillis(it.startedAt) }.toSortedSet()
    var longest = 0
    var run = 0
    var previous: Long? = null
    for (day in days) {
        run = if (previous != null && day - previous == DAY_MS) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    val today = dayStartMillis(nowMillis)
    var current = 0
    var cursor = if (days.contains(today)) today else today - DAY_MS
    while (days.contains(cursor)) {
        current++
        cursor -= DAY_MS
    }
    return StreakInfo(current = current, longest = longest)
}

/**
 * Books ranked by time spent reading, then pages turned. Only books still in
 * the library are listed, so a removed title can't linger here. Pure for
 * testability.
 */
internal fun topBooks(
    sessions: List<ReadingSession>,
    comics: List<Comic>,
    limit: Int = TOP_BOOK_LIMIT,
): List<TopBook> {
    if (sessions.isEmpty()) return emptyList()
    val byId = comics.associateBy { it.id }
    val aggregated = sessions
        .groupBy { it.comicId }
        .mapNotNull { (comicId, list) ->
            val comic = byId[comicId] ?: return@mapNotNull null
            TopBook(
                comic = comic,
                durationMs = list.sumOf { it.durationMs },
                pagesTurned = list.sumOf { it.pagesTurned },
            )
        }
        .sortedWith(
            compareByDescending<TopBook> { it.durationMs }
                .thenByDescending { it.pagesTurned },
        )
    return aggregated.take(limit)
}

private const val DAY_MS = 24L * 60L * 60L * 1000L
private const val TOP_BOOK_LIMIT = 5
