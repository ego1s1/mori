package com.mori.feature.stats.impl

import com.mori.core.model.Comic
import com.mori.core.model.DailyReadingStat
import com.mori.core.model.ReadingStats

/** Selected chart window. */
enum class StatsRange {
    WEEK,
    MONTH,
    YEAR,
}

/** Consecutive-day reading streak, in local days. */
data class StreakInfo(
    val current: Int,
    val longest: Int,
)

/** One book's reading totals, for the top-books list. */
data class TopBook(
    val comic: Comic,
    val durationMs: Long,
    val pagesTurned: Int,
)

sealed interface StatsUiState {
    data object Loading : StatsUiState

    data class Success(
        val totals: ReadingStats,
        /** Buckets for the selected [range], oldest first for the chart axis. */
        val buckets: List<DailyReadingStat>,
        val range: StatsRange,
        val streak: StreakInfo,
        val topBooks: List<TopBook>,
    ) : StatsUiState
}

sealed interface StatsAction {
    data class SelectRange(val range: StatsRange) : StatsAction
}
